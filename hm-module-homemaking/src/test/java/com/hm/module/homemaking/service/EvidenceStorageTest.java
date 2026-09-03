package com.hm.module.homemaking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class EvidenceStorageTest {
    @TempDir Path folder;
    byte[] png(int width,int height) throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    @Test void validPhotoUsesRandomTenantPathAndCanBeRead() throws Exception {
        var storage=new EvidenceStorage(folder.toString());var photo=storage.save(7,png(10,10));assertTrue(photo.key().startsWith("7/"));assertEquals("image/png",photo.type());assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(storage.read(photo.key()))));
    }
    @Test void rejectsNonImagesAndOversizedDimensions() throws Exception {
        var storage=new EvidenceStorage(folder.toString());assertThrows(Exception.class,()->storage.save(1,"<script>bad</script>".getBytes()));assertThrows(Exception.class,()->storage.save(1,png(6001,1)));assertThrows(Exception.class,()->storage.read("../outside.png"));
    }
    @Test void rollbackRemovesUncommittedPhoto() throws Exception {
        var storage=new EvidenceStorage(folder.toString());org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try{var photo=storage.save(1,png(10,10));for(var s:org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations())s.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);assertFalse(Files.exists(folder.resolve(photo.key())));}
        finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
    }
}
