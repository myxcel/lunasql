package lunasql.misc;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;

/**
 * Commande PLAY <br>
 * (Interne) Lecture d'un son
 * 
 * @author M.P.
 */
public class CmdPlay extends Instruction {

   public CmdPlay(Contexte cont) {
      super(cont, TYPE_CMDPLG, "PLAY", null);
   }

   @Override
   public int execute() {
      if (getLength() == 1) return cont.erreur("PLAY", "un nom de fichier son est attendu", lng);

      File fson = new File(getArg(1));
      if (!fson.canRead())
         return cont.erreur("PLAY", "le fichier son '" + getArg(1) + "' est inaccessible", lng);
       try {
          // Merci à http://stackoverflow.com/questions/18942424/error-playing-audio-file-from-java-via-pulseaudio-on-ubuntu
         AudioInputStream audio = AudioSystem.getAudioInputStream(fson);
         DataLine.Info info = new DataLine.Info(Clip.class, audio.getFormat());
         Clip clip = (Clip)AudioSystem.getLine(info);
         clip.open(audio);
         clip.start();

         cont.setValeur(null);
         return RET_CONTINUE;
       }
       catch (UnsupportedAudioFileException|LineUnavailableException|IOException ex) {
          return cont.exception("PLAY", "ERREUR " + ex.getClass().getSimpleName() + " : " +
                      ex.getMessage(), lng, ex);
       }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  play        Joue un fichier son .wav\n";
   }
}// class
