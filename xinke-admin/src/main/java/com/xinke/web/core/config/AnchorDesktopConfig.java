package com.xinke.web.core.config;

import java.nio.file.*;
import java.util.Properties;
import java.io.InputStream;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;
import jakarta.servlet.MultipartConfigElement;

/** Dedicated TLS connector for the native client's authenticated API. */
@Configuration
public class AnchorDesktopConfig {
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> anchorDesktopTls(){
        return factory->{
            Path settings=Path.of(System.getProperty("xinke.anchor.tls","/home/deploy/anchor-tls/server.properties"));
            if(!Files.isRegularFile(settings))return;
            try(InputStream input=Files.newInputStream(settings)){
                Properties p=new Properties();p.load(input);
                Connector connector=new Connector("org.apache.coyote.http11.Http11NioProtocol");
                connector.setPort(Integer.parseInt(p.getProperty("port","443")));
                connector.setScheme("https");connector.setSecure(true);connector.setProperty("SSLEnabled","true");
                connector.setProperty("maxThreads","16");connector.setProperty("maxConnections","100");
                SSLHostConfig ssl=new SSLHostConfig();ssl.setProtocols("TLSv1.2,TLSv1.3");
                SSLHostConfigCertificate cert=new SSLHostConfigCertificate(ssl,SSLHostConfigCertificate.Type.RSA);
                cert.setCertificateKeystoreFile(p.getProperty("keystore"));cert.setCertificateKeystorePassword(p.getProperty("password"));cert.setCertificateKeystoreType("PKCS12");
                ssl.addCertificate(cert);connector.addSslHostConfig(ssl);factory.addAdditionalTomcatConnectors(connector);
            }catch(Exception ex){throw new IllegalStateException("App TLS configuration could not load",ex);}
        };
    }
    @Bean
    MultipartConfigElement multipartConfigElement(){MultipartConfigFactory factory=new MultipartConfigFactory();factory.setMaxFileSize(DataSize.ofMegabytes(96));factory.setMaxRequestSize(DataSize.ofMegabytes(100));return factory.createMultipartConfig();}
}
