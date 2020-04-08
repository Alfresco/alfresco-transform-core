package org.alfresco.transformer.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="transform")
public class GlobalProperties
{
    private String libreoffice;
    private String pdf_renderer;
    private ImageMagick imagemagick = new ImageMagick();

    public String getPdf_renderer()
    {
        return pdf_renderer;
    }

    public void setPdf_renderer(String pdf_renderer)
    {
        this.pdf_renderer = pdf_renderer;
    }

    public ImageMagick getImagemagick()
    {
        return imagemagick;
    }

    public void setImagemagick(ImageMagick imagemagick)
    {
        this.imagemagick = imagemagick;
    }

    public String getLibreoffice()
    {
        return libreoffice;
    }

    public void setLibreoffice(String libreoffice)
    {
        this.libreoffice = libreoffice;
    }

    public static class ImageMagick
    {
        private String exe;
        private String dyn;
        private String root;

        public String getExe()
        {
            return exe;
        }

        public void setExe(String exe)
        {
            this.exe = exe;
        }

        public String getDyn()
        {
            return dyn;
        }

        public void setDyn(String dyn)
        {
            this.dyn = dyn;
        }

        public String getRoot()
        {
            return root;
        }

        public void setRoot(String root)
        {
            this.root = root;
        }
    }
}
