package com.xinke.erp.service;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.io.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;

@Service
public class AnchorAppReleaseService {
    private final Path store,feed;
    private final PublicKey publicKey;
    public AnchorAppReleaseService(@Value("${live.anchor.release-store:/ruoyi/anchor-app-releases}") String store,@Value("${live.anchor.release-feed:/ruoyi/dist/anchor-app}") String feed){
        this.store=Path.of(store);this.feed=Path.of(feed);
        try(InputStream input=getClass().getResourceAsStream("/anchor-update-public-key.txt")){
            publicKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(new String(Objects.requireNonNull(input).readAllBytes(),StandardCharsets.UTF_8).trim())));
        }catch(Exception ex){throw new IllegalStateException("Release verification key is missing",ex);}
    }
    public synchronized List<Map<String,Object>> list() throws IOException {
        Files.createDirectories(store);List<Map<String,Object>> result=new ArrayList<>();
        try(var files=Files.list(store)){for(Path file:files.filter(p->p.getFileName().toString().matches("release-\\d+\\.\\d+\\.\\d+\\.json")).toList())result.add(JSON.parseObject(Files.readString(file)));}
        String current=currentVersion();
        for(Map<String,Object> row:result)row.put("current",Objects.equals(row.get("version"),current));
        result.sort((a,b)->compare(String.valueOf(b.get("version")),String.valueOf(a.get("version"))));return result;
    }
    public synchronized void upload(MultipartFile installer,MultipartFile manifest)throws Exception{
        if(installer.isEmpty()||installer.getSize()>96L*1024*1024||manifest.getSize()>65536)throw new ServiceException("安装包或签名清单大小无效");
        byte[] envelope=manifest.getBytes();JSONObject data=verify(envelope);String version=data.getString("version"),name=data.getString("file");
        Files.createDirectories(store);Path metadata=store.resolve("release-"+version+".json");
        if(Files.exists(metadata))throw new ServiceException("此版本已存在，请使用新的版本号");
        Path pending=Files.createTempFile(store,"upload-",".tmp");
        try{
            installer.transferTo(pending);
            if(!digest(pending).equalsIgnoreCase(data.getString("sha256")))throw new ServiceException("安装包摘要与签名清单不一致");
            if(!name.equals(installer.getOriginalFilename()))throw new ServiceException("安装包文件名与清单不一致");
            Files.move(pending,store.resolve(name));
            Files.write(store.resolve(version+".manifest.json"),envelope,StandardOpenOption.CREATE_NEW);
            JSONObject row=new JSONObject();row.put("version",version);row.put("file",name);row.put("sha256",data.getString("sha256"));row.put("notes",data.getString("notes"));row.put("status","DRAFT");row.put("uploadedBy",SecurityUtils.getUsername());row.put("uploadedAt",new Date());
            atomic(metadata,JSON.toJSONBytes(row));
        }finally{Files.deleteIfExists(pending);}
    }
    public synchronized String beginUpload(MultipartFile manifest,long total)throws Exception{
        if(total<1||total>96L*1024*1024||manifest.getSize()>65536)throw new ServiceException("安装包大小无效");
        byte[] envelope=manifest.getBytes();verify(envelope);Files.createDirectories(store.resolve("uploads"));
        String id=UUID.randomUUID().toString();JSONObject metadata=new JSONObject();metadata.put("owner",SecurityUtils.getUserId());metadata.put("total",total);metadata.put("created",System.currentTimeMillis());
        Files.write(store.resolve("uploads/"+id+".manifest"),envelope);Files.writeString(store.resolve("uploads/"+id+".json"),metadata.toJSONString());Files.createFile(store.resolve("uploads/"+id+".part"));return id;
    }
    public synchronized void chunk(String id,int index,MultipartFile chunk)throws Exception{
        JSONObject metadata=uploadMetadata(id);int length=Math.toIntExact(chunk.getSize());
        long offset=index*524288L,total=metadata.getLongValue("total");
        if(index<0||length<1||length>524288||offset+length>total||(offset+length<total&&length!=524288))throw new ServiceException("分片大小无效");
        Path part=store.resolve("uploads/"+id+".part");long size=Files.size(part);byte[] bytes=chunk.getBytes();
        if(size==offset){Files.write(part,bytes,StandardOpenOption.APPEND);return;}
        if(size>=offset+length){try(RandomAccessFile input=new RandomAccessFile(part.toFile(),"r")){byte[] existing=new byte[length];input.seek(offset);input.readFully(existing);if(MessageDigest.isEqual(existing,bytes))return;}}
        throw new ServiceException("分片顺序或内容不一致，请重新上传");
    }
    public synchronized void finishUpload(String id)throws Exception{
        JSONObject metadata=uploadMetadata(id);Path part=store.resolve("uploads/"+id+".part"),manifest=store.resolve("uploads/"+id+".manifest");
        if(Files.size(part)!=metadata.getLongValue("total"))throw new ServiceException("安装包尚未上传完整");
        String filename=verify(Files.readAllBytes(manifest)).getString("file");
        upload(new StoredMultipart(part,filename),new StoredMultipart(manifest,"latest.json"));
        Files.delete(part);Files.delete(manifest);Files.delete(store.resolve("uploads/"+id+".json"));
    }
    private JSONObject uploadMetadata(String id)throws IOException{
        if(id==null||!id.matches("[a-f0-9-]{36}"))throw new ServiceException("上传任务无效");
        Path file=store.resolve("uploads/"+id+".json");if(!Files.exists(file))throw new ServiceException("上传任务不存在");
        JSONObject metadata=JSON.parseObject(Files.readString(file));
        if(!Objects.equals(metadata.getLong("owner"),SecurityUtils.getUserId())||System.currentTimeMillis()-metadata.getLongValue("created")>86400000)throw new ServiceException("上传任务已过期或不属于当前账号");
        return metadata;
    }
    private static class StoredMultipart implements MultipartFile{
        private final Path path;private final String name;
        StoredMultipart(Path path,String name){this.path=path;this.name=name;}
        public String getName(){return name;}public String getOriginalFilename(){return name;}public String getContentType(){return "application/octet-stream";}
        public boolean isEmpty(){return getSize()==0;}public long getSize(){try{return Files.size(path);}catch(IOException e){throw new java.io.UncheckedIOException(e);}}
        public byte[] getBytes()throws IOException{return Files.readAllBytes(path);}public InputStream getInputStream()throws IOException{return Files.newInputStream(path);}
        public void transferTo(java.io.File destination)throws IOException{Files.copy(path,destination.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        public void transferTo(Path destination)throws IOException{Files.copy(path,destination,StandardCopyOption.REPLACE_EXISTING);}
    }
    public synchronized void publish(String version)throws Exception{
        JSONObject row=metadata(version);String current=currentVersion();
        if(current!=null&&compare(version,current)<=0)throw new ServiceException("发布版本必须高于当前网站版本");
        byte[] envelope=Files.readAllBytes(store.resolve(version+".manifest.json"));JSONObject signed=verify(envelope);
        Path installer=store.resolve(signed.getString("file"));if(!digest(installer).equalsIgnoreCase(signed.getString("sha256")))throw new ServiceException("安装包校验失败");
        Files.createDirectories(feed);Path destination=feed.resolve(signed.getString("file"));
        Path temp=Files.createTempFile(feed,"installer-",".tmp");try{Files.copy(installer,temp,StandardCopyOption.REPLACE_EXISTING);publicReadable(temp);Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
        atomic(feed.resolve("latest.json"),envelope);
        row.put("status","PUBLISHED");row.put("publishedBy",SecurityUtils.getUsername());row.put("publishedAt",new Date());atomic(store.resolve("release-"+version+".json"),JSON.toJSONBytes(row));
    }
    public synchronized void withdraw(String version)throws Exception{
        JSONObject row=metadata(version);
        if(Objects.equals(currentVersion(),version)){
            List<Map<String,Object>> previous=list().stream().filter(r->!version.equals(r.get("version"))&&"PUBLISHED".equals(r.get("status"))).toList();
            if(previous.isEmpty()){
                Path fallback=store.resolve("initial.manifest.json");
                if(!Files.exists(fallback))throw new ServiceException("没有可恢复的上一版清单");
                atomic(feed.resolve("latest.json"),Files.readAllBytes(fallback));
            }else atomic(feed.resolve("latest.json"),Files.readAllBytes(store.resolve(previous.get(0).get("version")+".manifest.json")));
        }
        row.put("status","WITHDRAWN");atomic(store.resolve("release-"+version+".json"),JSON.toJSONBytes(row));
    }
    public void rememberInitial()throws IOException{Files.createDirectories(store);if(Files.isRegularFile(feed.resolve("latest.json"))&&!Files.exists(store.resolve("initial.manifest.json")))Files.copy(feed.resolve("latest.json"),store.resolve("initial.manifest.json"));}
    public JSONObject verify(byte[] raw)throws Exception{
        JSONObject envelope=JSON.parseObject(raw);byte[] payload=Base64.getDecoder().decode(envelope.getString("payload"));
        Signature signature=Signature.getInstance("SHA256withRSA");signature.initVerify(publicKey);signature.update(payload);
        if(!signature.verify(Base64.getDecoder().decode(envelope.getString("signature"))))throw new ServiceException("更新清单签名无效");
        JSONObject data=JSON.parseObject(payload);String version=data.getString("version");
        if(version==null||!version.matches("\\d{1,5}\\.\\d{1,5}\\.\\d{1,5}")||!("xinke-anchor-metrics-agent-setup-"+version+".exe").equals(data.getString("file"))||!data.getString("sha256").matches("[a-fA-F0-9]{64}"))throw new ServiceException("更新清单字段无效");
        return data;
    }
    private JSONObject metadata(String version)throws IOException{if(version==null||!version.matches("\\d{1,5}\\.\\d{1,5}\\.\\d{1,5}"))throw new ServiceException("版本号无效");Path file=store.resolve("release-"+version+".json");if(!Files.exists(file))throw new ServiceException("版本不存在");return JSON.parseObject(Files.readString(file));}
    private String currentVersion()throws IOException{if(!Files.exists(feed.resolve("latest.json")))return null;try{return verify(Files.readAllBytes(feed.resolve("latest.json"))).getString("version");}catch(Exception ex){throw new ServiceException("当前更新清单无效，请先核对");}}
    private static String digest(Path path)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream stream=Files.newInputStream(path)){byte[] buffer=new byte[65536];int n;while((n=stream.read(buffer))>0)md.update(buffer,0,n);}return HexFormat.of().formatHex(md.digest());}
    private void atomic(Path path,byte[] value)throws IOException{Path temp=Files.createTempFile(path.getParent(),"manifest-",".tmp");try{Files.write(temp,value);if(path.getParent().equals(feed))publicReadable(temp);Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}}
    private static void publicReadable(Path file)throws IOException{try{Files.setPosixFilePermissions(file,java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));}catch(UnsupportedOperationException ignored){/* Windows uses inherited ACLs. */}}
    private static int compare(String a,String b){String[] x=a.split("\\."),y=b.split("\\.");for(int i=0;i<3;i++){int diff=Integer.compare(Integer.parseInt(x[i]),Integer.parseInt(y[i]));if(diff!=0)return diff;}return 0;}
}
