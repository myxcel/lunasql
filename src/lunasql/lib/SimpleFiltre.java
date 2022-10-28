package lunasql.lib;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A class that implements the Java FileFilter interface.
 * @author M.P.
 */
public class SimpleFiltre implements FilenameFilter {

   private String ext;
   private String descr;
   private boolean isdir;

   public SimpleFiltre(){
      this("", "", true);
   }

   public SimpleFiltre(String ext, String descr){
      this(ext, descr, true);
   }

   public SimpleFiltre(String ext, String descr, boolean isdir){
      this.ext = ext;
      this.descr = descr;
      this.isdir = isdir;
   }

   /**
    * Teste si le fichier donné en paramétre du dossier dir est accepté par le fitre
    * @param path le chemin
    * @return true si accept
    */
   public boolean accept(String path){
      return accept(new File(path));
   }

   /**
    * Teste si le fichier donné en paramétre du dossier dir est accepté par le fitre
    * @param file le fichier
    * @return true si accept
    */
   public boolean accept(File file){
      return this.isdir && file.getName().toLowerCase().endsWith(this.ext);
   }

   /**
    * Teste si le fichier donné en paramétre du dossier dir est accepté par le fitre
    * @param dir le chemin
    * @param name le nom du fichier
    * @return true si accept
    */
   public boolean accept(java.io.File dir, String name) {
      File file = new File(dir.getPath() + File.separatorChar + name);
      return file.exists() && accept(file);
   }

   public String getExt(){
      return this.ext;
   }

   public String getDescr(){
      return this.descr;
   }

   public boolean isDir(){
      return this.isdir;
   }

   public void setExt(String ext){
      this.ext = ext;
   }

   public void setDescr(String descr){
      this.descr = descr;
   }

   public void setDir(boolean isdir){
      this.isdir = isdir;
   }
}
