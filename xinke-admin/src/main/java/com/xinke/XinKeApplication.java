package com.xinke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author xinke
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class XinKeApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(XinKeApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  鑫客启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " __   __   __  __     \n" +
                "/\\ \\ /\\ \\ /\\ \\/\\ \\    \n" +
                "\\ `\\`\\/'/'\\ \\ \\/'/'   \n" +
                " `\\/ > <   \\ \\ , <    \n" +
                "    \\/'/\\`\\ \\ \\ \\\\`\\  \n" +
                "    /\\_\\\\ \\_\\\\ \\_\\ \\_\\\n" +
                "    \\/_/ \\/_/ \\/_/\\/_/");
    }
}
