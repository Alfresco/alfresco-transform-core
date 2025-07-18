/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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
package org.alfresco.transformer.probes;

import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import static org.alfresco.transformer.fs.FileManager.SOURCE_FILE;
import static org.alfresco.transformer.fs.FileManager.TARGET_FILE;
import static org.alfresco.transformer.fs.FileManager.TempFileProvider.createTempFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.AbstractTransformerController;
import org.alfresco.transformer.logging.LogEntry;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 *
 *             Provides the logic performing test transformations by the live and ready probes.
 *
 *             <p>
 *             <b>K8s probes</b>: A readiness probe indicates if the pod should accept request. <b>It does not indicate that a pod is ready after startup</b>. The liveness probe indicates when to kill the pod. <b>Both probes are called throughout the lifetime of the pod</b> and a <b>liveness probes can take place before a readiness probe.</b> The k8s <b>initialDelaySeconds field is not fully honoured</b> as it is multiplied by a random number, so is actually a maximum initial delay in seconds, but could be 0.
 *             </p>
 *
 *             <p>
 *             Live and readiness probes might do test transforms. The first 6 requests result in a transformation of a small test file. The average time is remembered, but excludes the first one which is normally longer. This is used in future requests to discover if transformations are becoming slower. The request also returns a non 200 status code resulting in the k8s pod being terminated, after a predefined number of transformations have been performed or if any transformation takes a long time. These are controlled by environment variables:
 *             </p>
 *             <ul>
 *             <li>livenessPercent - The percentage slower the small test transform must be to indicate there is a problem.</li>
 *             <li>livenessTransformPeriodSeconds - As liveness probes should be frequent, not every request should result in a test transformation. This value defines the gap between transformations.</li>
 *             <li>maxTransforms - the maximum number of transformation to be performed before a restart.</li>
 *             <li>maxTransformSeconds - the maximum time for a transformation, including failed ones.</li>
 *             </ul>
 */
@Deprecated
public abstract class ProbeTestTransform
{
    private final Logger logger = LoggerFactory.getLogger(ProbeTestTransform.class);

    private static final int AVERAGE_OVER_TRANSFORMS = 5;
    private final String sourceFilename;
    private final String targetFilename;
    private final long minExpectedLength;
    private final long maxExpectedLength;

    private int livenessPercent;
    private long probeCount;
    private int transCount;
    private long normalTime;
    private long maxTime = Long.MAX_VALUE;
    private long nextTransformTime;

    private final boolean livenessTransformEnabled;
    private final long livenessTransformPeriod;
    private final long maxTransformCount;
    private long maxTransformTime;

    private final AtomicBoolean initialised = new AtomicBoolean(false);
    private final AtomicBoolean readySent = new AtomicBoolean(false);
    private final AtomicLong transformCount = new AtomicLong(0);
    private final AtomicBoolean die = new AtomicBoolean(false);

    public int getLivenessPercent()
    {
        return livenessPercent;
    }

    public long getMaxTime()
    {
        return maxTime;
    }

    /**
     * See Probes.md for more info.
     *
     * @param expectedLength
     *            was the length of the target file during testing
     * @param plusOrMinus
     *            simply allows for some variation in the transformed size caused by new versions of dates
     * @param livenessPercent
     *            indicates that for this type of transform a variation up to 2 and a half times is not unreasonable under load
     * @param maxTransforms
     *            default values normally supplied by helm. Not identical so we can be sure which value is used.
     * @param maxTransformSeconds
     *            default values normally supplied by helm. Not identical so we can be sure which value is used.
     * @param livenessTransformPeriodSeconds
     *            default values normally supplied by helm. Not identical so we can be sure which value is used.
     */
    public ProbeTestTransform(AbstractTransformerController controller,
            String sourceFilename, String targetFilename, long expectedLength, long plusOrMinus,
            int livenessPercent, long maxTransforms, long maxTransformSeconds,
            long livenessTransformPeriodSeconds)
    {
        this.sourceFilename = sourceFilename;
        this.targetFilename = targetFilename;
        minExpectedLength = Math.max(0, expectedLength - plusOrMinus);
        maxExpectedLength = expectedLength + plusOrMinus;

        this.livenessPercent = (int) getPositiveLongEnv("livenessPercent", livenessPercent);
        maxTransformCount = getPositiveLongEnv("maxTransforms", maxTransforms);
        maxTransformTime = getPositiveLongEnv("maxTransformSeconds", maxTransformSeconds) * 1000;
        livenessTransformPeriod = getPositiveLongEnv("livenessTransformPeriodSeconds",
                livenessTransformPeriodSeconds) * 1000;
        livenessTransformEnabled = getBooleanEnvVar("livenessTransformEnabled", false);
    }

    private boolean getBooleanEnvVar(final String name, final boolean defaultValue)
    {
        try
        {
            return Boolean.parseBoolean(System.getenv(name));
        }
        catch (Exception ignore)
        {}
        return defaultValue;
    }

    private long getPositiveLongEnv(String name, long defaultValue)
    {
        long l = -1;
        String env = System.getenv(name);
        if (env != null)
        {
            try
            {
                l = Long.parseLong(env);
            }
            catch (NumberFormatException ignore)
            {}
        }
        if (l <= 0)
        {
            l = defaultValue;
        }
        logger.trace("Probe: {}={}", name, l);
        return l;
    }

    // We don't want to be doing test transforms every few seconds, but do want frequent live probes.
    public String doTransformOrNothing(HttpServletRequest request, boolean isLiveProbe)
    {
        // If not initialised OR it is a live probe and we are scheduled to to do a test transform.
        probeCount++;
        // TODO: update/fix/refactor liveness probes as part of ATS-138
        if (isLiveProbe && !livenessTransformEnabled)
        {
            return doNothing(true);
        }
        return (isLiveProbe && livenessTransformPeriod > 0 &&
                (transCount <= AVERAGE_OVER_TRANSFORMS || nextTransformTime < System.currentTimeMillis()))
                || !initialised.get()
                        ? doTransform(request, isLiveProbe)
                        : doNothing(isLiveProbe);
    }

    private String doNothing(boolean isLiveProbe)
    {
        String probeMessage = getProbeMessage(isLiveProbe);
        String message = "Success - No transform.";
        LogEntry.setStatusCodeAndMessage(OK.value(), probeMessage + message);
        if (!isLiveProbe && !readySent.getAndSet(true))
        {
            logger.trace("{}{}", probeMessage, message);
        }
        return message;
    }

    private String doTransform(HttpServletRequest request, boolean isLiveProbe)
    {
        checkMaxTransformTimeAndCount(isLiveProbe);

        long start = System.currentTimeMillis();

        if (nextTransformTime != 0)
        {
            do
            {
                nextTransformTime += livenessTransformPeriod;
            } while (nextTransformTime < start);
        }

        File sourceFile = getSourceFile(request, isLiveProbe);
        File targetFile = getTargetFile(request);
        executeTransformCommand(sourceFile, targetFile);

        long time = System.currentTimeMillis() - start;
        String message = "Transform " + time + "ms";
        checkTargetFile(targetFile, isLiveProbe, message);

        recordTransformTime(time);
        calculateMaxTime(time, isLiveProbe);

        if (time > maxTime)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR,
                    getMessagePrefix(isLiveProbe) +
                            message + " which is more than " + livenessPercent +
                            "% slower than the normal value of " + normalTime + "ms");
        }

        // We don't care if the ready or live probe works out if we are 'ready' to take requests.
        initialised.set(true);

        checkMaxTransformTimeAndCount(isLiveProbe);

        return getProbeMessage(isLiveProbe) + message;
    }

    private void checkMaxTransformTimeAndCount(boolean isLiveProbe)
    {
        if (die.get())
        {
            throw new TransformException(TOO_MANY_REQUESTS,
                    getMessagePrefix(isLiveProbe) + "Transformer requested to die. A transform took " +
                            "longer than " + (maxTransformTime / 1000) + " seconds");
        }

        if (maxTransformCount > 0 && transformCount.get() > maxTransformCount)
        {
            throw new TransformException(TOO_MANY_REQUESTS,
                    getMessagePrefix(isLiveProbe) + "Transformer requested to die. It has performed " +
                            "more than " + maxTransformCount + " transformations");
        }
    }

    private File getSourceFile(HttpServletRequest request, boolean isLiveProbe)
    {
        incrementTransformerCount();
        File sourceFile = createTempFile("source_", "_" + sourceFilename);
        request.setAttribute(SOURCE_FILE, sourceFile);
        try (InputStream inputStream = this.getClass().getResourceAsStream('/' + sourceFilename))
        {
            Files.copy(inputStream, sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new TransformException(INSUFFICIENT_STORAGE,
                    getMessagePrefix(isLiveProbe) + "Failed to store the source file", e);
        }
        long length = sourceFile.length();
        LogEntry.setSource(sourceFilename, length);
        return sourceFile;
    }

    private File getTargetFile(HttpServletRequest request)
    {
        File targetFile = createTempFile("target_", "_" + targetFilename);
        request.setAttribute(TARGET_FILE, targetFile);
        LogEntry.setTarget(targetFilename);
        return targetFile;
    }

    public void recordTransformTime(long time)
    {
        if (maxTransformTime > 0 && time > maxTransformTime)
        {
            die.set(true);
        }
    }

    public void calculateMaxTime(long time, boolean isLiveProbe)
    {
        if (transCount <= AVERAGE_OVER_TRANSFORMS)
        {
            // Take the average of the first few transforms as the normal time. The initial transform might be slower
            // so is ignored. Later ones are not included in case we have a gradual performance problem.
            String message = getMessagePrefix(isLiveProbe) + "Success - Transform " + time + "ms";
            if (++transCount > 1)
            {
                normalTime = (normalTime * (transCount - 2) + time) / (transCount - 1);
                maxTime = (normalTime * (livenessPercent + 100)) / 100;

                if ((!isLiveProbe && !readySent.getAndSet(
                        true)) || transCount > AVERAGE_OVER_TRANSFORMS)
                {
                    nextTransformTime = System.currentTimeMillis() + livenessTransformPeriod;
                    logger.trace("{} - {}ms+{}%={}ms", message, normalTime, livenessPercent,
                            maxTime);
                }
            }
            else if (!isLiveProbe && !readySent.getAndSet(true))
            {
                logger.trace(message);
            }
        }
    }

    protected abstract void executeTransformCommand(File sourceFile, File targetFile);

    private void checkTargetFile(File targetFile, boolean isLiveProbe, String message)
    {
        String probeMessage = getProbeMessage(isLiveProbe);
        if (!targetFile.exists() || !targetFile.isFile())
        {
            throw new TransformException(INTERNAL_SERVER_ERROR,
                    probeMessage + "Target File \"" + targetFile.getAbsolutePath() + "\" did not exist");
        }
        long length = targetFile.length();
        if (length < minExpectedLength || length > maxExpectedLength)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR,
                    probeMessage + "Target File \"" + targetFile.getAbsolutePath() +
                            "\" was the wrong size (" + length + "). Needed to be between " +
                            minExpectedLength + " and " + maxExpectedLength);
        }
        LogEntry.setTargetSize(length);
        LogEntry.setStatusCodeAndMessage(OK.value(), probeMessage + "Success - " + message);
    }

    private String getMessagePrefix(boolean isLiveProbe)
    {
        return Long.toString(probeCount) + ' ' + getProbeMessage(isLiveProbe);
    }

    private String getProbeMessage(boolean isLiveProbe)
    {
        return (isLiveProbe ? "Live Probe: " : "Ready Probe: ");
    }

    public void incrementTransformerCount()
    {
        transformCount.incrementAndGet();
    }

    public void setLivenessPercent(int livenessPercent)
    {
        this.livenessPercent = livenessPercent;
    }

    public long getNormalTime()
    {
        return normalTime;
    }
}
