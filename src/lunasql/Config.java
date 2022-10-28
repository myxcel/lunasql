/*
 * Config.java
 * Configuration et constantes publiques
 * @author M.P.
 */
package lunasql;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import lunasql.lib.Contexte;
import lunasql.sql.TypesSGBD;

public class Config {

   /* À propos */
   public static final String APP_VERSION_NAME  = "Quincella";
   public static final String APP_VERSION_NUM   = "4.9.3.0"; // modèle: n.n.n.n
   public static final String APP_DT_REVISION   = "28 octobre 2022";
   public static final String APP_AUTHOR_NAME   = "Micaël Paganotto";
   public static final String APP_AUTHOR_EMAIL  = "keybase.io/espritlibredev";
   public static final String APP_AUTHOR_PGP    = "aa77 7903 6281 d0e9 209b e8b9 2627 39eb a36c eb3e";

   /* Comportement général */
   public static final int     CF_MAX_CALL_DEEP = 200;    // profondeur maximale pour ALIAS et EXEC

   /* Contexte d'exécution */
   public static final int     CT_VERBOSE = Contexte.VERB_MSG;
   public static final boolean CT_EXIT_ON_ERROR = true;   // sortie sur erreur
   public static final int     CT_CONST_EDIT = 2;         // constantes ':' éditables en console
   public static final int     CT_SQL_UPDATE = 1;         // exécution de code SQL de mise-à-jour
   public static final String  CT_EVAL_ENGINE = "js";     // moteur d'évaluation par défaut

   /* Contexte d'exécution : répertoires de scripts par défaut (séparateur : ou ; selon plateforme) */
   public static final String  CT_SCRIPTS_PATH = "scripts" + File.pathSeparator + ".";
   public static final String  CT_PLUGINS_PATH = "plugins" + File.pathSeparator + ".";
   public static final String  CT_SQL_EXT = "(?i)sql|lsql|luna"; // regexp des extensions des scripts LunaSQL
   public static final String  CT_INIT_FILE = "init.sql";  // fichier LunaSQL d'initialisation
   public static final String  CT_BASES_FILE = "login-list.cfg"; // fichier INI de définition des bases
   public static final String  CT_BASES_PATH =             // chemin
         System.getProperty("user.home") + File.separator + CT_BASES_FILE;
   public static final String  CT_CONFIG_FILE = "config.cfg";  // fichier de configuration
   public static final boolean CT_SAVE_CONF = false;      // si on sauve la config en quittant
   public static final boolean CT_ALIAS_ARG = false;      // Alias prennent args de la ligne
   public static final boolean CT_INTERACT = false;       // Opération sur les fichiers interactive
   public static final boolean CT_ALLOW_RECUR = false;    // Autorisation des alias récursifs /!\
   public static final boolean CT_ALLOW_REDEF = false;    // Autorisation de redéfinition des commandes /!\
   public static final boolean CT_END_CMD_NL = true;      // fin des commandes : nouvelle ligne
   public static final int     CT_COL_MAX_WIDTH = 100;    // largeur max. des colonnes
   public static final boolean CT_SELECT_ARR = true;      // affichage tabulaire des SELECT
   public static final boolean CT_ADD_ROW_NB = true;      // ajout des numéros de ligne en tableau
   public static final String  CT_LOG_DIR = ".";          // répertoire des logs d'erreur
   public static final String  CT_LOG_FILE = "crashes-"
         + new SimpleDateFormat("yyyy-MM").format(new Date()) + ".log";  // Fichier des erreurs
   public static final boolean CT_LIST_SUBST = false;     // Support des substitutions par listes par §
   public static final int CT_SIGN_POLICY = 0;            // Politique de signature (0: rien, 1: signé, 2: confiance)

   /* Console lunasql */
   public static final int CS_HISTORY_SIZE = 1000;        // Taille de l'historique
   public static final String CS_HISTORY_FILE = "history";  // fichier d'historique de la console
   public static final String CS_HISTORY_DBL = "2";       // gestion de l'historique
   public static final String CS_PROMPT = "SQL";          // Prompt par défaut
   public static final boolean CS_BEEP_ON = false;        // Cloche (bip) qui ennerve sur erreur
   public static final boolean CS_COLOR_ON = true;        // Couleurs dans la console

   /* Serveur HTTP */
   public static final int SR_HTTP_PORT = 5862;           // Port d'écoute par défaut pour serveur HTTP (LUNA)

   /* Base de données */
   public static final int DB_DEFAUT_TYPE = TypesSGBD.TYPE_H2DB;
}
