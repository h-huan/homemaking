import java.sql.*;import java.nio.file.*;import java.io.*;import org.h2.tools.RunScript;
class MigrationCheck {
 public static void main(String[] args)throws Exception{
  try(var c=DriverManager.getConnection("jdbc:h2:mem:migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE","sa","")){
   for(String f:new String[]{".runtime/migration/source.sql","sql/mysql/hm-homemaking.sql",".runtime/migration/translated.sql"})try(var reader=Files.newBufferedReader(Path.of(f))){RunScript.execute(c,reader);}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT o.price_cents,o.paid_cents,o.worker_id,o.status,o.refunded_cents FROM hm_order o JOIN hm_worker w ON w.id=o.worker_id AND w.tenant_id=o.tenant_id WHERE o.id=100")){if(!r.next()||r.getInt(1)!=12345||r.getInt(2)!=12345||r.getLong(3)!=7||!r.getString(4).equals("ASSIGNED")||r.getInt(5)!=1000)throw new AssertionError("Legacy order mapping failed");}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM hm_worker_slot WHERE booking_id=100")){r.next();if(r.getInt(1)!=4)throw new AssertionError("Worker reservations lost");}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT name FROM hm_service_category WHERE id=9 AND tenant_id=1")){if(!r.next()||!r.getString(1).equals("Cleaning"))throw new AssertionError("Legacy category id lost");}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM hm_legacy_archive")){r.next();System.out.println("Legacy mapping PASS; price=12345 cents, refunded=1000 cents, worker profile=7 (user=2), reserved slots=4, archived rows="+r.getInt(1));}
   try(var reader=Files.newBufferedReader(Path.of(".runtime/migration/translated.sql"))){RunScript.execute(c,reader);throw new AssertionError("Replay must fail");}catch(SQLException expected){System.out.println("Replay refused PASS");}
  }
 }
}
