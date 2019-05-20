/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer.executors;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.alfresco.error.AlfrescoRuntimeException;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///////// THIS FILE WAS A COPY OF THE CODE IN alfresco-repository /////////////

/**
 * Makes use of the JodConverter library and an installed
 * OpenOffice application to perform OpenOffice-driven conversions.
 *
 * @author Neil McErlean
 */
public class JodConverterSharedInstance implements JodConverter
{
    private static final Logger logger = LoggerFactory.getLogger(JodConverterSharedInstance.class);

    private OfficeManager officeManager;
    private boolean isAvailable = false;

    // JodConverter's built-in configuration settings.
    //
    // These properties are set by Spring dependency injection at system startup in the usual way.
    // If the values are changed via the JMX console at runtime, then the subsystem will be stopped
    // and can be restarted with the new values - meaning that JodConverter will also be stopped and restarted.
    // Therefore there is no special handling required for changes to e.g. portNumbers which determines
    // the number of OOo instances there should be in the pool.
    //
    // Numeric parameters have to be handled as Strings, as that is what Spring gives us for missing values
    // e.g. if jodconverter.maxTasksPerProcess is not specified in the properties file, the value
    //      "${jodconverter.maxTasksPerProcess}" will be injected.

    private Integer maxTasksPerProcess;
    private String url;
    private String officeHome;
    private int[] portNumbers;
    private Long taskExecutionTimeout;
    private Long taskQueueTimeout;
    private File templateProfileDir;
    private Boolean enabled;
    private Long connectTimeout;

    private String deprecatedOooExe;
    private Boolean deprecatedOooEnabled;
    private int[] deprecatedOooPortNumbers;

    void setMaxTasksPerProcess(String maxTasksPerProcess)
    {
        Long l = parseStringForLong(maxTasksPerProcess.trim());
        if (l != null)
        {
            this.maxTasksPerProcess = l.intValue();
        }
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    void setOfficeHome(String officeHome)
    {
        this.officeHome = officeHome == null ? "" : officeHome.trim();
    }

    public void setDeprecatedOooExe(String deprecatedOooExe)
    {
        this.deprecatedOooExe = deprecatedOooExe == null ? "" : deprecatedOooExe.trim();
    }

    void setPortNumbers(String s)
    {
        portNumbers = parsePortNumbers(s, "jodconverter");
    }

    public void setDeprecatedOooPort(String s)
    {
        deprecatedOooPortNumbers = parsePortNumbers(s, "ooo");
    }

    private int[] parsePortNumbers(String s, String sys)
    {
        int[] portNumbers = null;
        s = s == null ? null : s.trim();
        if (s != null && !s.isEmpty())
        {
            StringTokenizer tokenizer = new StringTokenizer(s, ",");
            int tokenCount = tokenizer.countTokens();
            portNumbers = new int[tokenCount];
            for (int i = 0;tokenizer.hasMoreTokens();i++)
            {
                try
                {
                    portNumbers[i] = Integer.parseInt(tokenizer.nextToken().trim());
                }
                catch (NumberFormatException e)
                {
                    // Logging this as an error as this property would prevent JodConverter & therefore
                    // OOo from starting as specified
                    logger.error("Unparseable value for property '{}.portNumbers': {}", sys, s);
                    // We'll not rethrow the exception, instead allowing the problem to be picked up
                    // when the OOoJodConverter subsystem is started.
                }
            }
        }
        return portNumbers;
    }

    void setTaskExecutionTimeout(String taskExecutionTimeout)
    {
        this.taskExecutionTimeout = parseStringForLong(taskExecutionTimeout.trim());
    }

    void setTemplateProfileDir(String templateProfileDir)
    {
        if (templateProfileDir == null || templateProfileDir.trim().length() == 0)
        {
            this.templateProfileDir = null;
        }
        else
        {
            File tmp = new File(templateProfileDir);
            if (!tmp.isDirectory())
            {
                throw new AlfrescoRuntimeException("OpenOffice template profile directory "+templateProfileDir+" does not exist.");
            }
            this.templateProfileDir = tmp;
        }
    }

    void setTaskQueueTimeout(String taskQueueTimeout)
    {
        this.taskQueueTimeout = parseStringForLong(taskQueueTimeout.trim());
    }

    void setConnectTimeout(String connectTimeout)
    {
    	this.connectTimeout = parseStringForLong(connectTimeout.trim());
    }

    void setEnabled(final String enabledStr)
    {
        enabled = parseEnabled(enabledStr);

        // If this is a request from the Enterprise Admin console to disable the JodConverter.
        if (!enabled && (deprecatedOooEnabled == null || !deprecatedOooEnabled))
        {
            // We need to change isAvailable to false so we don't make calls to a previously started OfficeManger.
            // In the case of Enterprise it is very unlikely that ooo.enabled will have been set to true.
            isAvailable = false;
        }
    }

    public void setDeprecatedOooEnabled(String deprecatedOooEnabled)
    {
        this.deprecatedOooEnabled = parseEnabled(deprecatedOooEnabled);
        // No need to worry about isAvailable as this setting cannot be changed via the Admin console.
    }

    private Boolean parseEnabled(String enabled)
    {
        enabled = enabled == null ? "" : enabled.trim();
        return Boolean.parseBoolean(enabled);
    }

    // So that Community systems <= Alfresco 6.0.1-ea keep working on upgrade, we may need to use the deprecated
    // ooo.exe setting rather than the jodconverter.officeHome setting if we don't have the jod setting as
    // oooDirect was replaced by jodconverter after this release.
    private String getOfficeHome()
    {
        String officeHome = this.officeHome;
        if ((officeHome == null || officeHome.isEmpty()) && (deprecatedOooExe != null && !deprecatedOooExe.isEmpty()))
        {
            // It will only be possible to use the ooo.exe value if it includes a path, which itself has the officeHome
            // value in it.

            // jodconverter.officeHome=/opt/libreoffice5.4/
            //                 ooo.exe=/opt/libreoffice5.4/program/soffice.bin

            // jodconverter.officeHome=C:/noscan/installs/521~1.1/LIBREO~1/App/libreoffice
            //                 ooo.exe=C:/noscan/installs/COMMUN~1.0-E/LIBREO~1/App/libreoffice/program/soffice.exe

            File oooExe = new File(deprecatedOooExe);
            File parent = oooExe.getParentFile();
            if (parent != null && "program".equals(parent.getName()))
            {
                File grandparent = parent.getParentFile();
                if (grandparent != null)
                {
                    officeHome = grandparent.getPath();
                }
            }
        }
        return officeHome;
    }

    // So that Community systems <= Alfresco 6.0.1-ea keep working on upgrade, we may need to use the deprecated
    // ooo.enabled setting if true rather than the jodconverter.enabled setting as oooDirect was replaced by
    // jodconverter after this release.
    //     If ooo.enabled is true the JodConverter will be enabled.
    //     If ooo.enabled is false or unset the jodconverter.enabled value is used.
    //     Community set properties via alfresco-global.properties.
    //     Enterprise may do the same but may also reset jodconverter.enabled them via the Admin console.
    //     In the case of Enterprise it is very unlikely that ooo.enabled will be set to true.
    private boolean isEnabled()
    {
        return (deprecatedOooEnabled != null && deprecatedOooEnabled) || (enabled != null && enabled);
    }

    // So that Community systems <= Alfresco 6.0.1-ea keep working on upgrade, we may need to use the deprecated
    // ooo.port setting rather than the jodconverter.portNumbers if ooo.enabled is true and jodconverter.enabled
    // is false.
    private int[] getPortNumbers()
    {
        return (enabled == null || !enabled) && deprecatedOooEnabled != null && deprecatedOooEnabled
            ? deprecatedOooPortNumbers
            : portNumbers;
    }

    private Long parseStringForLong(String string)
    {
        try
        {
            return Long.parseLong(string);
        }
        catch (NumberFormatException nfe)
        {
            logger.debug("Cannot parse numerical value from {}", string);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.content.JodConverter#isAvailable()
     */
    public boolean isAvailable()
    {
        return isAvailable && (officeManager != null || (url != null && !url.isEmpty()));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @PostConstruct
    public void afterPropertiesSet()
    {
    	// isAvailable defaults to false afterPropertiesSet. It only becomes true on successful completion of this method.
    	this.isAvailable = false;

        int[] portNumbers = getPortNumbers();
        String officeHome = getOfficeHome();
        if (logger.isDebugEnabled())
        {
            logger.debug("JodConverter settings (null settings will be replaced by jodconverter defaults):");
            logger.debug("  officeHome = {}", officeHome);
            logger.debug("  enabled = {}", isEnabled());
            logger.debug("  portNumbers = {}", getString(portNumbers));
            logger.debug("    ooo.exe = {}", deprecatedOooExe);
            logger.debug("    ooo.enabled = {}", deprecatedOooEnabled);
            logger.debug("    ooo.port = {}", getString(deprecatedOooPortNumbers));
            logger.debug("    jodConverter.enabled = {}", enabled);
            logger.debug("    jodconverter.portNumbers = {}", getString(this.portNumbers));
            logger.debug("  jodconverter.officeHome = {}", this.officeHome);
            logger.debug("  jodconverter.maxTasksPerProcess = {}", maxTasksPerProcess);
            logger.debug("  jodconverter.taskExecutionTimeout = {}", taskExecutionTimeout);
            logger.debug("  jodconverter.taskQueueTimeout = {}", taskQueueTimeout);
            logger.debug("  jodconverter.connectTimeout = {}", connectTimeout);
            logger.debug("  jodconverter.url = {}", url);
        }

        // Only start the JodConverter instance(s) if the subsystem is enabled.
        if (!isEnabled())
        {
            return;
        }

        if (url == null || url.isEmpty())
        {

            logAllSofficeFilesUnderOfficeHome();

            try
            {
                DefaultOfficeManagerConfiguration defaultOfficeMgrConfig = new DefaultOfficeManagerConfiguration();
                if (maxTasksPerProcess != null && maxTasksPerProcess > 0)
                {
                    defaultOfficeMgrConfig.setMaxTasksPerProcess(maxTasksPerProcess);
                }
                if (officeHome != null && officeHome.length() != 0)
                {
                    defaultOfficeMgrConfig.setOfficeHome(officeHome);
                }
                if (portNumbers != null && portNumbers.length != 0)
                {
                    defaultOfficeMgrConfig.setPortNumbers(portNumbers);
                }
                if (taskExecutionTimeout != null && taskExecutionTimeout > 0)
                {
                    defaultOfficeMgrConfig.setTaskExecutionTimeout(taskExecutionTimeout);
                }
                if (taskQueueTimeout != null && taskQueueTimeout > 0)
                {
                    defaultOfficeMgrConfig.setTaskQueueTimeout(taskQueueTimeout);
                }
                if (templateProfileDir != null)
                {
                    defaultOfficeMgrConfig.setTemplateProfileDir(templateProfileDir);
                }
                if (connectTimeout != null)
                {
                    defaultOfficeMgrConfig.setConnectTimeout(connectTimeout);
                }
                // Try to configure and start the JodConverter library.
                officeManager = defaultOfficeMgrConfig.buildOfficeManager();
                officeManager.start();
            }
            catch (IllegalStateException e)
            {
                logger.error("Unable to pre-initialise JodConverter library. " +
                             "The following error is shown for informational purposes only.", e);
                return;
            }
            catch (OfficeException e)
            {
                logger.error("Unable to start JodConverter library. " +
                             "The following error is shown for informational purposes only.", e);

                // We need to let it continue (comment-out return statement) even if an error occurs. See MNT-13706 and associated issues.
                //return;
            }
            catch (Exception e)
            {
                logger.error(
                    "Unexpected error in configuring or starting the JodConverter library." +
                    "The following error is shown for informational purposes only.", e);
                return;
            }
        }

        // If any exceptions are thrown in the above code, then isAvailable
        // should remain false, hence the return statements.
        this.isAvailable = true;
    }

    private String getString(int[] portNumbers)
    {
        StringBuilder portInfo = new StringBuilder();
        if (portNumbers != null)
        {
            for (int i = 0;i < portNumbers.length;i++)
            {
                portInfo.append(portNumbers[i]);
                if (i < portNumbers.length - 1)
                {
                    portInfo.append(", ");
                }
            }
        }
        return portInfo.toString();
    }

    private void logAllSofficeFilesUnderOfficeHome()
    {
    	if (!logger.isDebugEnabled())
    	{
    		return;
    	}

    	String officeHome = getOfficeHome();
    	File requestedOfficeHome = new File(officeHome);

    	logger.debug("Some information on soffice* files and their permissions");

    	logFileInfo(requestedOfficeHome);

    	for (File f : findSofficePrograms(requestedOfficeHome, new ArrayList<>(), 2))
    	{
    		logFileInfo(f);
    	}
    }

    private List<File> findSofficePrograms(File searchRoot, List<File> results, int maxRecursionDepth)
    {
    	return this.findSofficePrograms(searchRoot, results, 0, maxRecursionDepth);
    }

    private List<File> findSofficePrograms(File searchRoot, List<File> results,
    		int currentRecursionDepth, int maxRecursionDepth)
    {
    	if (currentRecursionDepth >= maxRecursionDepth)
    	{
    		return results;
    	}

    	File[] matchingFiles = searchRoot.listFiles((dir, name) -> name.startsWith("soffice"));
        results.addAll(asList(matchingFiles));

        for (File dir : requireNonNull(searchRoot.listFiles(File::isDirectory)))
    	{
    		findSofficePrograms(dir, results, currentRecursionDepth + 1, maxRecursionDepth);
    	}

    	return results;
    }

    /**
     * Logs some information on the specified file, including name and r/w/x permissions.
     * @param f the file to log.
     */
    private void logFileInfo(File f)
    {
    	if (!logger.isDebugEnabled())
    	{
    		return;
    	}

    	StringBuilder msg = new StringBuilder();
    	msg.append(f).append(" ");
    	if (f.exists())
    	{
    		if (f.canRead())
    		{
    			msg.append("(")
    			   .append(f.isDirectory() ? "d" : "-")
    			   .append(f.canRead() ? "r" : "-")
    			   .append(f.canWrite() ? "w" : "-")
    			   .append(f.canExecute() ? "x" : "-")
    			   .append(")");
    		}
    	}
    	else
    	{
    		msg.append("does not exist");
    	}
    	logger.debug(msg.toString());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    @PreDestroy
    public void destroy()
    {
	    this.isAvailable = false;
	    if (officeManager != null)
	    {
	    	// If there is an OfficeException when stopping the officeManager below, then there is
	    	// little that can be done other than logging the exception and carrying on. The JodConverter-based
	    	// libraries will not be used in any case, as isAvailable is false.
	    	//
	    	// Any exception thrown out of this method will be logged and swallowed by Spring
	    	// (see javadoc for method declaration). Therefore there is no handling here for
	    	// exceptions from jodConverter.
	        officeManager.stop();
	    }
	}

    /* (non-Javadoc)
     * @see org.alfresco.repo.content.JodConverterWorker#getOfficeManager()
     */
    @Override
    public OfficeManager getOfficeManager()
    {
        return officeManager;
    }
}
