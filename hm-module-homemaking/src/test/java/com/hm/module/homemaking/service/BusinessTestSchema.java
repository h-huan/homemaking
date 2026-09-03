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
        } catch(IOException e){throw new UncheckedIOException(e);}
    }
}
