package com.hm.module.homemaking.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import javax.sql.DataSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

final class BusinessTestSchema {
    private BusinessTestSchema() {}
    static void payment(DataSource source) {
        try {
            // Domain tests load the actual business DDL; admin/menu DML is verified on the full acceptance schema.
            String script=Files.readString(Path.of("../sql/mysql/upgrades/V005__payment_modes.sql")).split("INSERT INTO system_menu",2)[0];
            var populator=new ResourceDatabasePopulator(new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)));
            populator.setSqlScriptEncoding("UTF-8");populator.execute(source);
            String changes=Files.readString(Path.of("../sql/mysql/upgrades/V006__order_changes.sql")).split("INSERT INTO system_menu",2)[0];
            var upgrade=new ResourceDatabasePopulator(new ByteArrayResource(changes.getBytes(StandardCharsets.UTF_8)));upgrade.setSqlScriptEncoding("UTF-8");upgrade.execute(source);
            String workbench=Files.readString(Path.of("../sql/mysql/upgrades/V008__worker_workbench.sql")).split("INSERT INTO system_menu",2)[0];
            var workerUpgrade=new ResourceDatabasePopulator(new ByteArrayResource(workbench.getBytes(StandardCharsets.UTF_8)));workerUpgrade.setSqlScriptEncoding("UTF-8");workerUpgrade.execute(source);
            String completion=Files.readString(Path.of("../sql/mysql/upgrades/V009__customer_completion_confirmation.sql"));
            var confirmationUpgrade=new ResourceDatabasePopulator(new ByteArrayResource(completion.getBytes(StandardCharsets.UTF_8)));confirmationUpgrade.setSqlScriptEncoding("UTF-8");confirmationUpgrade.execute(source);
            String aftersale=Files.readString(Path.of("../sql/mysql/upgrades/V010__aftersale_workflow.sql")).split("INSERT INTO system_menu",2)[0];
            var aftersaleUpgrade=new ResourceDatabasePopulator(new ByteArrayResource(aftersale.getBytes(StandardCharsets.UTF_8)));aftersaleUpgrade.setSqlScriptEncoding("UTF-8");aftersaleUpgrade.execute(source);
            String reviews=Files.readString(Path.of("../sql/mysql/upgrades/V011__complete_reviews.sql")).split("INSERT INTO system_menu",2)[0];
            var reviewUpgrade=new ResourceDatabasePopulator(new ByteArrayResource(reviews.getBytes(StandardCharsets.UTF_8)));reviewUpgrade.setSqlScriptEncoding("UTF-8");reviewUpgrade.execute(source);
        } catch(IOException e){throw new UncheckedIOException(e);}
    }
}
