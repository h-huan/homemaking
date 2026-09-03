package com.hm.module.homemaking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import static com.hm.module.homemaking.dal.HmRepository.check;

/** Private evidence is never uploaded to a public file bucket or served by Nginx. */
@Service
public class EvidenceStorage {
    public record Saved(String key,String type) {}
    private final Path root;
    public EvidenceStorage(@Value("${hm.homemaking.evidence-root:./data/homemaking-evidence}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    public Saved save(long tenant,byte[] bytes){
        check(bytes!=null&&bytes.length>0&&bytes.length<=5*1024*1024,"履约照片须为不超过 5 MB 的 JPEG 或 PNG");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);check(readers.hasNext(),"无法识别图片格式");var reader=readers.next();
            try{
                String format=reader.getFormatName().toLowerCase();check(format.equals("jpeg")||format.equals("png"),"履约照片仅支持 JPEG 或 PNG");reader.setInput(input,true,true);
                int width=reader.getWidth(0),height=reader.getHeight(0);check(width>0&&height>0&&width<=6000&&height<=6000&&(long)width*height<=24000000,"图片尺寸过大，请缩小后上传");
                var image=reader.read(0);String key=tenant+"/"+UUID.randomUUID()+(format.equals("jpeg")?".jpg":".png");Path target=resolve(key);Files.createDirectories(target.getParent());
                // Re-encoding removes EXIF location and extraneous data from the uploaded image.
                try(var output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){check(ImageIO.write(image,format,output),"图片编码失败");}
                catch(Exception e){Files.deleteIfExists(target);throw e;}
                if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(target);}catch(IOException ignored){}}});
                return new Saved(key,format.equals("jpeg")?"image/jpeg":"image/png");
            }finally{reader.dispose();}
        }catch(IOException e){throw new IllegalStateException("履约照片保存失败，请检查私有存储目录",e);}
    }
    public byte[] read(String key){try{return Files.readAllBytes(resolve(key));}catch(IOException e){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"履约照片不存在");}}
    private Path resolve(String key){Path path=root.resolve(key).normalize();check(path.startsWith(root)&&!path.equals(root),"履约照片路径无效");return path;}
}
