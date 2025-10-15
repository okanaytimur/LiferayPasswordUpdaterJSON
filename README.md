LiferayPasswordUpdaterJSON

Bu proje, Oracle veritabanındaki Liferay user_ tablosunda bulunan kullanıcıların şifrelerini değiştirmek ve passwordencrypted alanını sıfırlamak için hazırlanmış bir Java programıdır.

Program terminal üzerinden çalışır ve backup oluşturma, update, commit işlemlerini yapar.

Gereksinimler

Java 8 veya üzeri JDK/JRE (Windows için CMD veya PowerShell)

Oracle JDBC sürücüsü (ojdbc8.jar)

Oracle internationalization kütüphanesi (orai18n.jar) — veritabanı karakter seti WE8ISO8859P9 ise gerekli.

Proje Dosya Yapısı
eews/
 ├─ LiferayPasswordUpdaterJSON.java
 ├─ LiferayPasswordUpdaterJSON.class   (derlenmiş hali)
 ├─ ojdbc8.jar
 ├─ orai18n.jar
 ├─ dbconfig.json

Veritabanı Bağlantısı

dbconfig.json örneği:

{
  "db_url": "jdbc:oracle:thin:@//dbhost:1521/ORCL",
  "db_user": "DBUSER",
  "db_pass": "DBPASSWORD"
}


db_url, db_user, db_pass değerlerini kendi Oracle ortamına göre değiştirin.

Dikkat: Canlı veritabanında çalıştırmadan önce test ortamında yedekleme ve test yapın.

Derleme ve Çalıştırma (Windows CMD)
1. Derleme

Java 8 ile uyumlu derlemek için:

javac -source 1.8 -target 1.8 -cp .;ojdbc8.jar;orai18n.jar LiferayPasswordUpdaterJSON.java


Bu, .class dosyasını oluşturur.

Eğer javac komutu tanınmıyorsa JDK kurulu değil veya PATH’e ekli değil.

2. Çalıştırma

CMD’de:

java -cp .;ojdbc8.jar;orai18n.jar LiferayPasswordUpdaterJSON


Program terminalden screenName sorar.

Ardından yeni şifre girmeniz istenir (input gizli).

Kullanıcı bulunursa:

CSV backup oluşturulur (örn: user_backup_<screenName>_YYYYMMDD_HHMMSS.csv)

password_ güncellenir, passwordencrypted sıfırlanır

Commit yapılır

Kullanıcı bulunamazsa: program abort eder.

Hata durumunda rollback yapılır ve hata mesajı terminalde gösterilir.


JDK

https://www.oracle.com/tr/java/technologies/downloads/#jdk25-windows
