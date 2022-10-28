/*********************************************************************
          Didacticiel de démonstration pour LunaSQL V4

  Usage :
    SQL> exec tutorial.sql

  Auteur : Micaël P.
  Date création : 13/07/2014
  Date révision : 09/01/2019
  Version : 1.3
 *********************************************************************/

need 4.8;
opt -l :END_CMD_NL 1;

use test       -- pour check
use doc-h2     -- pour help-h2doc
use doc-sql    -- pour w3s-sql

-- Initialisation
verb MESSAGE
opt -l :EXIT_ON_ERR 1
opt -l :ON_ERROR {
  print -e "$<n>Erreurs survenues dans le didacticiel. Désolée."
  sortie
}
def -l pause {
  if $[str eq? $[input "+++$<n>"] fin] sortie
}
def -l sortie {
  DROP TABLE IF EXISTS $(tb_clients)
  DROP TABLE IF EXISTS $(tb_produits)
  DROP TABLE IF EXISTS $(tb_commandes)
  exit $(*err_lng?0) "$<n>À très bientôt dans ce didacticiel !"
}

-- Contrôles de la base de données
opt -l :LIST_SUBSTIT 0
check-eq _DB_TYPE H2DB
def -l tb_clients    TUTO_CLIENTS
def -l tb_commandes  TUTO_COMMANDES
def -l tb_produits   TUTO_PRODUITS

if $[select count(*) from INFORMATION_SCHEMA.TABLES ^
     where TABLE_SCHEMA='PUBLIC' and TABLE_NAME like 'TUTO_%']>0 {
  print -e "Les tables existent déjà."
  print "Il est recommandé de lancer le tutoriel sur une base vide.
Si vous continuez, les tables  $(tb_clients), $(tb_commandes) et
$(tb_produits) seront écrasées."
  if '$[input "Voulez-vous continuer (o/n) ? "]'!='o' sortie
}

----------------------------------------------------------------------

-- Bienvenue dans le didacticiel !
print " * Bienvenue dans le didacticiel de démonstration de LunaSQL *

LunaSQL est une application en mode console offrant d'exécuter en base
de données des commandes SQL, mais aussi d'interpréter des commandes
utilitaires et des scripts. L'objectif est d'automatiser des tâches
d'administration de la base ou de traitement des données, voire de
prototyper une application de base de données avant son implantation
dans la technologie définitive.

Voyons le détail de ce qu'elle propose.
Les +++ en début de ligne signifient 'Appuyez sur ENTRÉE' ; il est
possible à tout moment de sortir du didacticiel en tapant 'fin'."
pause

-- Généralités sur l'application
print "Bien sûr, LunaSQL est tout d'abord faite pour exécuter des...
commandes SQL. Donc elle se connecte à une base de données (H2, HSQL,
Derby, Oracle, SQLServer, MS Access...) et permet d'y lancer des
commandes SQL. Pour le didacticiel, la base est H2.
Jouons donc avec quelques commandes en console !"
pause

print "Avant de commencer, quelques précisions sur les commandes
(pour plus de détails, voir l'aide 'syntax') :
  - les commandes s'écrivent au prompt, à raison d'une commande par
    ligne, ou bien séparées par un ';'. Si une commande ouvre une
    parenthèse non fermée avant la fin de la ligne, ou un crochet ou
    un accolade, ou une chaîne, ou bien si la ligne se termine par ^,
    alors la commande continue sur la (ou les) ligne(s) suivante(s).
  - il y en a trois types : les commandes SQL (ex. SELECT, DROP...),
    les commandes internes supplémentaires (ex. PRINT) et les macros
    et alias dont nous verrons l'utilité dans quelques instants.
  - les commandes internes et SQL s'écrivent en majuscule ou minuscule
    (on peut taper indifféremment SELECT ou select, PRINT ou print),
    mais les macros et alias (qui sont des variables) sont sensibles
    à la casse à la déclaration.
  - pour obtenir de l'aide sur une commande, taper 'help <commande>'
  - la liste complète des commandes s'obtient par un 'help commands'
  - la liste complète des aides disponibles s'obtient par un 'help'
  - les commentaires sont introduits par -- sur une ligne, ou bien
    entre /* et */ pour ceux qui s'étendent sur plusieurs lignes
  - certains caractères (comme ^") peuvent être échappés par ^ "
pause


print "Exercice :
Ouvrez donc une autre console LunaSQL (note : si le message abscons
'Locked by another process' s'affiche, alors il est nécessaire de
relancer la connexion avec l'option : AUTO_SERVER=TRUE), et tapez les
commandes suivantes (sans le SQL> initial qui représente l'invite de
saisie, et sans non plus ce qui suit les --, c'est un commentaire).
SQL> help        -- aide générale
... (aide) ..."
pause

print "Ceci est la page d'index de l'aide de LunaSQL. Différentes sous-
rubriques sont disponibles ; nous en verrons quelques-unes. N'hésitez
pas à tout moment à abuser de la commande 'help' ! En outre, pour un
aperçu des commandes disponibles, tapez :
SQL> help commands
Toutes les commandes sont ainsi listées, à commencer par les commandes
SQL, puis les commandes internes à LunaSQL, et enfin les 'Variables
utilisateur', c'est-à-dire les alias et macros définies par vos soins
(nous y reviendrons dans un court instant).

Enfin, l'utilisation générale de LunaSQL et des chaînes de caractères
est détaillée sous les rubriques :
SQL> help syntax
... (aide) ...
SQL> help delimiters
... (aide) ..."
pause

----------------------------------------------------------------------

print "$<n>Voyons tout de suite quelques commandes utiles !

1) La commande d'affichage de message : PRINT
Elle imprime immédiatement à l'écran tous ses arguments
SQL> help print  -- aide de la commande PRINT
... (aide) ...
SQL> print ^"Bonjour Monde !^"
Bonjour Monde !
Essayez maintenant d'afficher le même message mais en jaune."
pause
print "Réponse :$<n>SQL> print -c=4 ^"Bonjour Monde !^""
pause

print "2) La commande d'affectation de variables : DEF
SQL> help def  -- aide de la commande DEF
... (aide) ...
SQL> def x ^"Lorem ipsum dolor sit amet, consectetur adipiscing elit^"
SQL> print ^"x contient : ^$(x)^""
def x "Lorem ipsum dolor sit amet, consectetur adipiscing elit"
print "x contient : $(x)"
undef x
pause

print "Notes :
  - une variable peut être supprimée par la commande UNDEF
  - une variable déclarée avec l'option -l est locale au bloc dans
    lequel elle est définie et ses blocs fils (consulter l'aide 
    'variables' pour plus d'information)
  - l'option bien pratique -a ajoute l'argument à une variable déjà
    définie (locale ou globale)
  - ^$(x) renvoie la valeur de x, nous y reviendrons"
pause

print "3) La commande de demande de saisie : INPUT
Elle s'utilise généralement avec la commande d'affectation DEF
SQL> help input  -- aide de la commande INPUT
... (aide) ...
SQL> def x ^$[input ^"Saisissez la valeur : ^"]"
def x $[input -i "Saisissez la valeur : "]
print "SQL> print ^"Vous avez saisi : ^$(x)^""
print "Vous avez saisi : $(x)
Vous aurez ici encore noté que ^$(x) renvoie la valeur de la variable
'x' ; patience, nous reviendrons sur les substitutions."
undef x
pause

print "4) La commande d'exécution de script LunaSQL : EXEC
SQL> help exec  -- aide de la commande EXEC
... (aide) ...
SQL> exec ^"/home/moi/chemin/vers/commandes.sql^"     -- Unix
SQL> exec ^"c:\users\moi\chemin\vers\commandes.sql^"  -- Windows
Remplacez bien-sûr le chemin par un fichier existant chez vous, par
exemple pour tester un fichier nommé test.sql contenant :
print ^"Le fichier test.sql est exécuté !^"
Le fichier peut évidemment contenir toute commande SQL ou LunaSQL."
pause

print "5) La commande d'exécution de commande système : SHELL
SQL> help shell  -- aide de la commande SHELL
... (aide) ...
SQL> shell ls -al -- Unix
SQL> shell dir    -- Windows"
pause

print "6) La commande de redirection de la sortie vers fichier : SPOOL
SQL> help spool  -- aide de la commande SPOOL
... (aide) ...
SQL> spool ^"/home/moi/chemin/vers/sortie.txt^"     -- Unix
SQL> spool ^"c:\users\moi\chemin\vers\sortie.txt^"  -- Windows
Le fichier 'sortie.txt' va être créé et contiendra toutes les sorties
des commandes SQL (SELECT...) et LunaSQL (PRINT...).
SQL> print ^"à ajouter en fichier de sortie^"
SQL> spool off  -- pour revenir en console"
pause

print "7) La commande pour gérer le niveau de messages : VERB
SQL> help verb  -- aide de la commande VERB
... (aide) ...
SQL> verb   -- affiche le niveau actuel (normal : MESSAGE)
SQL> verb BAVARD  -- là on va voir beaucoup de messages
SQL> verb 1       -- maintenant uniquement ceux qu'on demande"
pause

print "8) La commande pour gérer les options de la console : OPT
Chaque option est gérée par une constante de paramètrage. C'est une
variable commençant par ':'. En général, elle décrit un comportement
particulier de la console LunaSQL. D'autres commencent par '_' et sont
utilisées en interne à l'application (non modifiables directement).
SQL> help opt  -- aide de la commande OPT
... (aide) ...
SQL> opt          -- affiche toutes les constantes de paramétrage
SQL> opt :PROMPT  -- affiche le contenu de l'option 'PROMPT'
SQL> opt :PROMPT Allez-y  -- changement du prompt (invite)"
pause

print "$<n>Voilà quelques commandes LunaSQL usuelles indépendantes de
la base de données (pas d'interaction avec les tables). Il en existe
bien d'autres, nous les détaillerons un peu après."
pause

----------------------------------------------------------------------

-- Commandes SQL
print "$<n>Abordons maintenant un peu ce qui nous intéresse : le SQL !
Sans proposer un cours complet de SQL, nous allons voir rapidement
quelques exemples d'utilisation de LunaSQL avec des commandes SQL.

Pour de l'aide générale sur la langage SQL, la paquet 'doc-sql'
définit des macros d'ouverture de documentation en ligne.
Pour de l'aide sur la syntaxe et les mots-clef SQL de la base H2,
le paquet 'doc-h2' contient aussi quelques macros utiles.
(Un paquet contient des fonctionnalités additionnelles et s'importe
par la commande USE, nous y reviendrons en fin de didacticiel...)
Pour plus d'information sur le SQL de votre SGBD, reportez-vous à la
documentation fournie avec votre SGBD.
SQL> use doc   -- importe les paquets 'doc-sql' et 'doc-h2'
SQL> help-doc-h2
... (aide) ..."
pause

print "La macro w3s-sql ouvre une page d'aide de syntaxe SQL :
SQL> w3s-sql  -- paquet 'doc-sql' : outils divers et aides SQL"
w3s-sql
pause

print "$<n>On commence par créer et remplir un jeu de tables bateau :
les trois tables $(tb_clients), $(tb_produits) et $(tb_commandes).
La commande SQL à utiliser est CREATE TABLE."
pause

-- Création des tables
-- Table CLIENTS
print "SQL> CREATE TABLE NOT IF EXISTS $(tb_clients) (
  ID INT PRIMARY KEY,
  NOM VARCHAR(50),
  ADR VARCHAR(255)
)"
CREATE TABLE IF NOT EXISTS $(tb_clients) (
  ID INT PRIMARY KEY, NOM VARCHAR(50), ADR VARCHAR(255))
pause

-- Table PRODUITS
print "SQL> CREATE TABLE IF NOT EXISTS $(tb_produits) (
  ID INT PRIMARY KEY,
  LIBELLE VARCHAR(255),
  PRIX DECIMAL
)"
CREATE TABLE IF NOT EXISTS $(tb_produits) (
  ID INT PRIMARY KEY, LIBELLE VARCHAR(255), PRIX DECIMAL)
pause

-- Table COMMANDES
print "SQL> CREATE TABLE IF NOT EXISTS $(tb_commandes) (
  ID INT PRIMARY KEY,
  IDCLI INT NOT NULL REFERENCES $(tb_clients)(ID),
  IDPRO INT NOT NULL REFERENCES $(tb_produits)(ID),
  DTCOM DATE NULL
)"
CREATE TABLE IF NOT EXISTS $(tb_commandes) (
  ID INT PRIMARY KEY, IDCLI INT NOT NULL REFERENCES $(tb_clients)(ID),
  IDPRO INT NOT NULL REFERENCES $(tb_produits)(ID), DTCOM DATE NULL)
pause

print "$<n>Vérifions ensuite la bonne existence des tables créées :
C'est la commande interne SHOW
SQL> show $(tb_clients)|$(tb_produits)|$(tb_commandes)"
show $(tb_clients)|$(tb_produits)|$(tb_commandes)
pause

print "La création des tables a l'air d'avoir fonctionné...
La commande SHOW liste les tables dont le nom correspond au patron
regexp fourni en argument (ou toutes si pas d'argument)."
pause

-- Suppression du contenu éventuel
print "$<n>Par sécurité, il peut être pas mal de vider les tables.
Utilisons la commande SQL DELETE :
SQL> DELETE FROM $(tb_commandes)
SQL> DELETE FROM $(tb_clients)
SQL> DELETE FROM $(tb_produits)"
DELETE FROM $(tb_commandes)
DELETE FROM $(tb_clients)
DELETE FROM $(tb_produits)
pause

----------------------------------------------------------------------

-- Remplissage des tables : requêtes INSERT
print "$<n>Insérons maintenant quelques données aux tables fraîchement
créées et nettoyées. La commande SQL INSERT va nous aider :
SQL> INSERT INTO $(tb_clients) VALUES (
  1, 'Théo Rifumeuse', '99 rue des philosophes, Strasbourg'), (
  2, 'Amour Hirederire', '13 passage de l humour, Colmar'), (
  3, 'Daisy Dratet', '21 av. du désert, Altkirch'), (
  4, 'Sissi Lindrenvé', '51b rue du moteur pété, Mulhouse')"
INSERT INTO $(tb_clients) VALUES (
  1, 'Théo Rifumeuse', '99 rue des philosophes, Strasbourg'), (
  2, 'Amour Hirederire', '13 passage de l humour, Colmar'), (
  3, 'Daisy Dratet', '21 av. du désert, Altkirch'), (
  4, 'Sissi Lindrenvé', '51b rue du moteur pété, Mulhouse')
pause

print "SQL> INSERT INTO $(tb_produits) VALUES (
  1, 'CD de Leilou Lacuisse en concert', 29.95), (
  2, 'DVD d apprentissage de la poterie antique', 19.90),(
  3, 'Manuel de montage d un moulin à meule', 15.50),(
  4, 'Méthode d apprentissage ^"L Alsacien facile^"', 33.70),(
  5, 'Apprendre à chanter pour les plus nuls', 25.35)"
INSERT INTO $(tb_produits) VALUES (
  1, 'CD de Leilou Lacuisse en concert', 29.95), (
  2, 'DVD d apprentissage de la poterie antique', 19.90),(
  3, 'Manuel de montage d un moulin à meule', 15.50),(
  4, 'Méthode d apprentissage "L Alsacien facile"', 33.70),(
  5, 'Apprendre à chanter pour les plus nuls', 25.35)
pause

print "SQL> INSERT INTO $(tb_commandes) VALUES (
  1, 2, 5, '2013-10-12'), (
  2, 1, 3, '2013-11-05'), (
  3, 4, 4, '2013-11-19'), (
  4, 4, 1, '2013-12-01'), (
  5, 3, 2, '2013-12-18'), (
  6, 1, 4, '2013-12-30'), (
  7, 2, 3, '2014-01-07'), (
  8, 3, 5, '2014-01-07'), (
  9, 1, 3, '2014-10-24')"
INSERT INTO $(tb_commandes) VALUES (
  1, 2, 5, '2013-10-12'), (2, 1, 3, '2013-11-05'), (
  3, 4, 4, '2013-11-19'), (4, 4, 1, '2013-12-01'), (
  5, 3, 2, '2013-12-18'), (6, 1, 4, '2013-12-30'), (
  7, 2, 3, '2014-01-07'), (8, 3, 5, '2014-01-07'), (
  9, 1, 3, '2014-10-24')
pause

----------------------------------------------------------------------

-- Quelques sélections pour l'apéritif
print "$<n>Les tables sont prêtes à être utilisées. Nous avons appris
les commandes CREATE TABLE et INSERT, voyons maintenant comment
utiliser la commande SQL SELECT pour des faire des sélections.
L'aide SQL en w3s-sql détaille toutes les commandes SQL, ce tutoriel
n'a pas pour objectif de constituer une référence en SQL."
pause

print "Échauffons-nous avec quelques commandes SELECT simples.
 - Nombre de produits coûtant moins de 30 E :
SQL> SELECT COUNT(*) AS NB FROM $(tb_produits) WHERE PRIX < 30.00"
SELECT COUNT(*) AS NB FROM $(tb_produits) WHERE PRIX < 30.00
pause

print " - Nom et adresse de tous les clients (sans filtre) :
SQL> SELECT NOM, ADR FROM $(tb_clients)"
SELECT NOM, ADR FROM $(tb_clients)
pause

print " - Caractéristiques des produits coûtant 20 E et plus :
SQL> SELECT * FROM $(tb_produits) WHERE PRIX >= 20.00"
SELECT * FROM $(tb_produits) WHERE PRIX > 20.00
pause

print " - Toutes les commandes passées par le client 1 en 2013 :
SQL> SELECT C.NOM, P.LIBELLE $<c>
FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P $<c>
WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.ID=1 AND YEAR(D.DTCOM)=2014"
SELECT C.NOM, P.LIBELLE ^
FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P ^
WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.ID = 1 AND YEAR(D.DTCOM) = 2014
pause

print " - Montant total des recettes en 2013 :
SQL> SELECT SUM(P.PRIX) FROM $(tb_commandes) D, $(tb_produits) P $<c>
WHERE D.IDPRO=P.ID AND YEAR(D.DTCOM) = 2013"
SELECT SUM(P.PRIX) FROM $(tb_commandes) D, $(tb_produits) P ^
WHERE D.IDPRO=P.ID AND YEAR(D.DTCOM) = 2013
pause

----------------------------------------------------------------------

-- Un soupçon de requêtes UPDATE pour le plat principal
print "$<n>Ça va suffire pour les requêtes SELECT. Passons rapidement à d'autres
types de commandes SQL utiles : UPDATE pour modifier des lignes.
Les prix des produits ont augmenté de 5% suite à une nouvelle taxe :
SQL> UPDATE $(tb_produits) SET PRIX = PRIX * 1.05"
UPDATE $(tb_produits) SET PRIX = PRIX * 1.05
pause

print "Il y a eu une erreur à la saisie de la commande No. 9 :
elle n'a pas eue lieu le 24/10/2014 mais le 23/10/2014 :
SQL> UPDATE $(tb_commandes) SET DTCOM = '2014-10-23' WHERE ID = 9"
UPDATE $(tb_commandes) SET DTCOM = '2014-10-23' WHERE ID = 9
pause

print "Exercice :
dans une autre console SQL, déterminez pour combien a acheté la cliente
Daisy Dratet en 2014"
pause

def x $[SEEK SELECT SUM(P.PRIX) ^
    FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P ^
    WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = 'Daisy Dratet' ^
          AND YEAR(D.DTCOM) = 2014]
print "Réponse : $(x)
Résultat déterminé par la requête :
SELECT SUM(P.PRIX)
  FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P
 WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND
       C.NOM = 'Daisy Dratet' AND YEAR(D.DTCOM) = 2014"
undef x
pause

print "$<n>D'autres commandes SQL sont bien-sûr supportées, comme INSERT pour
l'ajout de lignes, DELETE pour la suppression, ou encore TRUNCATE pour
la purge d'une table, CALL pour l'évaluation d'une expression, ALTER
pour la modification de la structure d'une table... Ces commandes
appartiennent généralement au standard SQL, et une bonne documentation
sur le langage SQL pour apprendra tout ce que vous devez savoir.

Ce qui serait pas mal maintenant, ce serait d'aborder quelques commandes
propres à LunaSQL qui nous aident pour la manipulation de SGBD !
Intégrons donc les commandes SQL dans des commandes internes, hao ma ?"
pause

----------------------------------------------------------------------

print "$<n>Voyons tout de suite quelques commandes bien pratiques !

1) La commande d'infos sur les tables de l'utilisateur : SHOW
Nous l'avons entrevue un peu plus tôt. Elle liste les tables dont le
nom correspond au patron regexp fourni en argument (ou toutes si pas
d'argument).
SQL> help show  -- aide de la commande SHOW
... (aide) ...
SQL> show   -- sans argument, elle liste les tables et leur taille"
show
print "SQL> show $(tb_clients) -- avec un nom de table, affiche la structure"
pause

print "2) La commande d'importation de données dans une table : IMPORT
SQL> help import  -- aide de la commande IMPORT
... (aide) ...
SQL> import ^"/home/moi/chemin/vers/donnees.csv^" $(tb_clients)
Remplacez bien-sûr le chemin par un fichier contenant des données,
par exemple un fichier donnees.csv contenant :
ID;NOM;ADR
5;Alain Térieure;47 bd de la boîte fermée, Saint Louis
6;Candy Tonthoubib;2bis chemin de l'Hôpital, Thann
7;Tad Bozieux-Thussay;99 rue de Beau Regard, Cernay"
pause

print "3) La commande d'exportation de données d'une table : EXPORT
SQL> help export  -- aide de la commande EXPORT
... (aide) ...
SQL> export ^"/home/moi/chemin/vers/donnees.csv^" $(tb_clients)
Le contenu de la table $(tb_clients) va être écrit en fichier CSV
donnees.csv. On peut aussi exporter le résultat d'uene requête SQL :
SQL> export ^"/home/moi/chemin/vers/donnees.csv^" $<c>
            ^"SELECT * FROM $(tb_clients) WHERE ID<5^"
Essayez de jouer avec les différents formats d'export !"
pause

print "4) La commande d'affichage du contenu d'une table : DISP
SQL> help disp  -- aide de la commande DISP
... (aide) ...
SQL> disp $(tb_clients)
Vous remarquerez que cela équivaut à SELECT * FROM $(tb_clients) !
En fait à la place du nom de table, on peut spécifier un patron
de noms de tables (expression régulière). Essayez !"
pause

print "5) La commande d'affichage de la taille d'une table : SIZE
SQL> help size  -- aide de la commande SIZE
... (aide) ...
SQL> size $(tb_clients)
Là aussi, on peut spécifier un patron de noms de tables."
pause

print "6) La commande de recherche de valeur sur requête SQL : SEEK
SQL> help seek  -- aide de la commande SEEK
... (aide) ...
Elle est typiquement utilisée pour retourner une valeur :
SQL> seek SELECT count(*) FROM $(tb_clients) WHERE NOM like 'A%'"
seek SELECT count(*) FROM $(tb_clients) WHERE NOM like 'A%'
print "Mais peut aussi retourner une structure plus complexe !
SQL> seek SELECT NOM, ADR FROM $(tb_clients) WHERE NOM like 'A%'"
seek SELECT NOM, ADR FROM $(tb_clients) WHERE NOM like 'A%'
print "Notez que le retour est une liste de structures {COLONNE=valeur} !"
pause

print "$<n>Voilà pour un petit tour de quelques commandes SQL et LunaSQL utiles
pour la manipulation des données de notre base de données.
Pour rappel :
  - la liste complète des commandes s'obtient par 'help commands'
  - la liste complète des aides disponibles s'obtient par 'help'
  - l'aide spécifique à une commande s'obtient par 'help <cmd>'"
pause

----------------------------------------------------------------------

-- Les substitutions en LunaSQL
print "$<n>Abordons ce que veut dire ^$(x) (vous l'attendiez, hein ?)

Un (des ?) point fort de LunaSQL est la diversité des substitutions
(c'est-à-dire des bouts de code qui peuvent être remplacés par des
valeurs au moment de l'exécution). Les variables en sont un exemple :
nous les avons introduites tout-à-l'heure avec la notation ^$(x), mais
il en existe d'autres types.
Jetez donc un oeil à l'aide 'substitutes' (pour les généralités), et à
l'aide 'expressions' (pour approfondir les expressions substituables) :
SQL> help substitutes
... (aide) ...
SQL> help expressions
... (aide) ..."
pause

print "En bref, voici un récapitulatif des substitutions possibles :
  - ^$() remplace le nom de la variable par sa valeur
    SQL> def x foo; print ^"x vaut ^$(x)^""
def x foo; print "x vaut $(x)"
undef x
pause

print "  - ^$[] remplace l'expression par sa valeur finale
    SQL> print ^"PI = ^$[4*Math.atan(1)]^"  -- utilise le moteur JS"
print "PI = $[4*Math.atan(1)]"
pause

print "  - ^$[] s'applique en fait aussi à toute commande SQL ou LunaSQL
    SQL> def x bar; print ^"x vaut aussi ^$[def x]^""
def x bar; print "x vaut aussi $[def x]"
undef x
pause

print "    En fait, toute commande qui retourne une valeur peut être utilisée
    au sein de la structure de substitution ^$[]."
pause

print "  - ^$[] remplace donc aussi la commande SQL par son résultat :
    SQL> print ^"Taille de la table $(tb_clients) :^" $<c>
               ^$[SELECT COUNT(*) FROM $(tb_clients)]"
print "Taille de la table $(tb_clients) :" ^
      $[SELECT COUNT(*) FROM $(tb_clients)]
pause

print "    Notez bien que si la commande SELECT (ou CALL) venait à retourner
    plusieurs colonnes ou plusieurs lignes, seule la valeur en A1 est
    utilisée par ^$[]. Essayez donc de compléter la commande précédente
    pour vérifier le comportement de ^$[] avec plusieurs colonnes !"
pause

print "  - ^$`` remplace l'invite par la saisie utilisateur
    SQL> print ^"Donc selon vous, ^$`votre opinion ? `^""
print "Donc selon vous, $[input -i ^"votre opinion ? ^"]"
pause

print "Exercices pour réviser tout ça :
1) Sans utiliser la commande input, demandez à l'utilisateur de saisir
un mot de passe, mais les caractères ne doivent pas s'afficher !
2) En utilisant la commande input, essayez de trouver un équivalent
pour faire saisir du texte puis un mot de passe en console."
pause

print "Réponses :
SQL> print ^"Votre MDP: ^$`*Saisir le MDP: `^"
SQL> print ^"Votre txt: ^$[input -i ^^"Saisir le texte: ^^"]^"
SQL> print ^"Votre MDP: ^$[input -ip ^^"Saisir le MDP: ^^"]^"
Dans cette version, l'option -i empêche les substitutions."
pause

----------------------------------------------------------------------

-- Définition d'une macro perso
print "$<n>Nous avons parlé tout-à-l'heure de la commande DEF, qui permet
de définir des variables en associant une valeur à un nom de variable.
Il faut savoir que cette valeur, qui est dans tous les cas une chaîne
de caractères, peut aussi contenir du code LunaSQL exécutable !
Il y en a deux types : les alias et les macros."
pause

print "Voyons cela en exemples :
On souhaite disposer d'un moyen de connaître le montant total des
recettes selon une année donnée, qui peut varier.
Définissons donc l'alias suivant :
SQL> def get-montant $<c>
    ^"SELECT SUM(P.PRIX) FROM $(tb_commandes) $<c>
    WHERE D.IDPRO=P.ID AND YEAR(D.DTCOM) = ^""
def get-montant ^
  "SELECT SUM(P.PRIX) FROM $(tb_commandes) D, $(tb_produits) P ^
  WHERE D.IDPRO=P.ID AND YEAR(D.DTCOM) = "
pause

print "Comment utiliser cet alias ?
Le nom 'get-montant' va pouvoir être appelé directement en console,
suivi de l'argument attendu (l'année). Puisqu'il va concaténer les
arguments de l'appel à lui-même, il faut que la constante d'option
:ALIAS_ARG soit positionnée à 1. Allons-y :
SQL> opt :ALIAS_ARG 1   -- on peut paramétrer ça au démarrage
SQL> get-montant 2014"
opt -l :ALIAS_ARG 1
get-montant 2014
opt -l :ALIAS_ARG 0
pause

print "Remarque : la syntaxe d'appel en préfixant l'alias par ':' donne
le même résultat sans avoir à positionner :ALIAS_ARG à 1.
SQL> :get-montant 2014"
:get-montant 2014
undef get-montant
pause

print "$<n>Assez pratique, non ? Mais comment faire si le paramètre variable est
*dans* la chaîne, et non à la fin ? C'est là que les macros viennent à
notre aide. À la différence des alias, les macros ne concatènent pas
les arguments de l'appel, mais utilisent des variables *locales* pour y
accéder. Ce fonctionnement est beaucoup plus puissant. "
pause

print "Voyez plutôt :
On cherche à connaître combien a dépensé un client donné lors d'une
année donnée. On ne peut pas utiliser d'alias car il y a deux
paramètres à passer. Définissons donc une macro (qui va au passage
servir de solution à un exercice ci-dessus) :
SQL> def get-montant-client $<c>
  ^"SELECT SUM(D.PRIX) $<c>
  FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P $<c>
  WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = '^^$(arg1)' $<c>
        AND YEAR(D.DTCOM) = ^^$(arg2)^""
pause
def get-montant-client ^
  "SELECT SUM(P.PRIX) ^
  FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P ^
  WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = '^$(arg1)' ^
        AND YEAR(D.DTCOM) = ^$(arg2)"

print "Comment utiliser cette macro ?
Comme l'alias, elle va pouvoir être appelée directement en console,
suivie des arguments attendus (le nom de client et l'année). Mais
:ALIAS_ARG doit être à 0, car les variables locales arg1 et arg2 sont
là pour transmettre les paramètres. Il ne faut pas que les valeurs
soient en plus concaténées à la fin de la requête.
SQL> opt :ALIAS_ARG 0
SQL> get-montant-client ^"Daisy Dratet^" 2014"
opt -l :ALIAS_ARG 0
get-montant-client "Daisy Dratet" 2014
undef get-montant-client
pause

print "$<n>Encore plus pratique, non ? Plusieurs choses à remarquer :
  - Cet usage des macros permet de placer les paramètres où on veut
  - ^"Daisy Dratet^" est entre ^" car on n'attend qu'un paramètre et
  l'espace est séparateur d'arguments.
  - La macro a été définie comme une chaîne, entre des ^". Cela oblige
  les arguments arg1 et arg2 à être échappés, c'est-à-dire précédés de
  ^ pour éviter qu'ils soient substitués à la définition. Astuce :
  définir les macros en les encadrant par { et }, qui diffèrent les
  substitutions,
  - Les arguments peuvent être préparés par la commande ARG,
  - Si vous voulez sortir d'une macro avant la fin de son exécution,
  utilisez la commande EXIT,
  - contrairement aux commandes internes (DEF, PRINT...) et SQL, mais
  comme les variables, les alias et les macros sont dépendants de la
  casse."
pause

print "Introduisons la commande de gestion des arguments : ARG
Elle ne s'utilise que dans une macro ou un fichier script.
SQL> help arg  -- aide de la commande ARG
... (aide) ..."
pause

print "Voici donc la version améliorée de la macro get-montant-client :
On utilise ici la commande ARG, qui gère elle-même les contraintes des
arguments, ainsi que les arguments optionnels, valeurs par défaut...
SQL> def get-montant-client {
  arg nom annee
  SELECT SUM(D.PRIX) $<c>
  FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P $<c>
  WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = '^$(nom)' $<c>
        AND YEAR(D.DTCOM) = ^$(annee)
}"
pause

print "$<n>Il existe une macro pour déclarer facilement des macros,
il s'agit de defmacro ! Jugez plutôt pour get-montant-client :
SQL> defmacro get-montant-client {.nom .annee:int} {
  SELECT SUM(D.PRIX) $<c>
  FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P $<c>
  WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = '^$(.nom)' $<c>
        AND YEAR(D.DTCOM) = ^$(.annee)
} ^"Documentation affichée par help get-montant-client
   qui est très utile pour intégrer une doc à notre macro !^""
pause

print "$<n>Vous suivez toujours ? Bien !
Voyons à ce point d'autres commandes bien pratiques, qui visent à
contrôler le flux d'exécution.

1) La commande d'exécution conditionnelle de bloc : IF
SQL> help if  -- aide de la commande IF
... (aide) ...
SQL> if [ 1<2 ] { print ^"ouf! l'arithmétique est respectée^" }"
if [ 1<2 ] { print "ouf! l'arithmétique est respectée" }
pause

print "Il est conseillé d'encadrer le bloc à exécuter par { et }, bien que
cela ne soit pas obligatoire. De même les [] encadrant l'expression
booléenne ne sont pas obligatoires, mais assurent qu'elle n'est passée
qu'en un seul argument (le premier de la commande if)."
pause

print "2) La commande d'exécution conditionnelle contextuelle : WHEN
SQL> help when  -- aide de la commande WHEN
... (aide) ...
SQL> when [ 1<2 ]
-- toutes les commandes qui vont suivre, jusqu'au ELSE ou au END,
-- seront exécutées, car 1<2 est vrai !
SQL> print ^"l'arithmétique est encore respectée^"
SQL> end  -- à ne pas oublier"
when [ 1<2 ]; print "l'arithmétique est encore respectée"; end
pause

print "3) La commande d'exécution en boucle de bloc : FOR
SQL> help for  -- aide de la commande FOR
... (aide) ...
SQL> for i [1 2 3 4] { print ^$i }"
for i [1 2 3 4] { print $i }
pause
print "On veut maintenant lister tous les produits sur une seule ligne
séparés par des virgules. FOR avec l'option -q permet de passer une
requête SQL SELECT ou bien simplement le nom d'une table.
SQL> def result ^"^"
SQL> for -q $(tb_produits) { def result ^$(result) ^$(col2), }
SQL> def result   -- contenu de result"
def result ""
for -q $(tb_produits) { def result $(result)$(col2), }
def result
pause

print "Notez que l'option -n couplée à l'option -q permet d'utiliser dans le
bloc à exécuter les noms de colonne récupérées par la requête. Rejouons
la précédente commande (avec en bonus l'usage de DEF -a) :
SQL> def result ^"^"
SQL> for -qn $(tb_produits) { def -a result ^$(LIBELLE), }
SQL> def result   -- contenu de result"
def result ""
for -qn $(tb_produits) { def -a result ^$(LIBELLE), }
def result
undef result
pause

print "Je vous concède que ces derniers exemples sont d'un intérêt limité,
mais en combinant tous ces usages, on peut faire pas mal de choses
intéressantes."
pause

print "$<n>Et si l'on revenait sur la 1re définition de get-montant-client ?
Sa définition était :
def get-montant-client $<c>
  ^"SELECT SUM(D.PRIX) $<c>
  FROM $(tb_clients) C INNER JOIN $(tb_commandes) D ON C.ID=D.IDCLI, $<c>
       $(tb_commandes) D INNER JOIN $(tb_produits) P ON D.IDPRO=P.ID $<c>
  WHERE C.NOM = '^$(arg1)' AND YEAR(D.DTCOM) = ^$(arg2)^"
Ici, arg1 et arg2 *doivent* être définis, c'est-à-dire que pour éviter
une erreur, il faut passer au moins 2 arguments. Ça fait boum s'il n'y
en a qu'un.
Exercice : comment modifier get-montant-client pour tester le nombre
d'arguments saisis avant le SELECT ?"
pause

print "Vous voulez la réponse ? Allez :
SQL> def get-montant-client {
  if [^$(arg_nb) < 2] { error ^"Nombre d'arguments incorrect^" }
  SELECT SUM(P.PRIX) $<c>
  FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P $<c>
  WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = '^$(arg1)' $<c>
        AND YEAR(D.DTCOM) = ^$(arg2)
}"
def get-montant-client {
  if [$(arg_nb) < 2] { print -e "Nombre d'arguments incorrect" } else {
    SELECT SUM(P.PRIX) ^
    FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P ^
    WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = '$(arg1)' ^
          AND YEAR(D.DTCOM) = $(arg2) }
}
pause

print "Essayons notre version plus robuste avec test du nombre d'arguments :
SQL> get-montant-client ^"Daisy Dratet^""
get-montant-client "Daisy Dratet"
pause
print "SQL> get-montant-client ^"Daisy Dratet^" 2014"
get-montant-client "Daisy Dratet" 2014
undef get-montant-client
pause

print "Cet exemple est pédagogique, car en vrai il est bien-sûr préférable
d'utiliser la commande ARG pour gérer les arguments saisis, ou mieux comme
nous l'avons vu, utiliser la macro defmacro (qui elle-même utilise ARG) :

SQL> defmacro get-montant-client {.nom .annee:int} {
  SELECT SUM(P.PRIX) $<c>
  FROM $(tb_clients) C, $(tb_commandes) D, $(tb_produits) P $<c>
  WHERE C.ID=D.IDCLI AND D.IDPRO=P.ID AND C.NOM = '^$(.nom)' $<c>
        AND YEAR(D.DTCOM) = ^$(.annee)
}"
pause

----------------------------------------------------------------------

-- Les commandes utilitaires
print "$<n>Abordons rapidement les commandes utilitaires.
Ce sont des commandes fonctionnelles qui permettent de réaliser des
traitements simples sur des données. Voici la liste complète :
  - STR  : traitements divers sur des chaînes de caractères
  - LIST : traitements divers sur des listes de chaînes
  - DICT : traitements divers sur des dictionnaires (clé=valeur)
  - FILE : traitements divers sur des fichiers (lecture, écriture...)
  - TIME : traitements divers sur des dates
Consultez la documentation de ces commandes pour en savoir plus !"
pause

-- Les modules (packages)
print "$<n>Un mot sur les paquets.
LunaSQL peut s'enrichir de fonctionnalités en y chargeant des scripts
fournis avec. Essayez 'help packages' pour la liste complète.
L'aide de chaque paquet chargé est disponible par 'help-<nom_paquet>'

Revenons donc sur la documentation H2 (fonctions en module doc-h2).
Vous souvenez-vous des paquets 'doc' et 'doc-h2' abordés plus haut ?
Ils ont été chargés pour le didacticiel par la commande USE, qui
importe dans le contexte global des bibliothèques de macros ou de
fonctions Javascript additionnelles :
SQL> help use
... (aide) ...
SQL> use doc-h2
SQL> use sys  -- un autre module utile"
pause

print "Parcourez les macros qu'ils contiennent par les commandes
help-h2doc et help-sys !"
pause

----------------------------------------------------------------------

-- Le SE et commandes JS
print "$<n>Abordons enfin les notions de Script engine et de Javascript.
Pour faire simple, disons seulement que Java, le langage dans lequel
est écrit LunaSQL, est livré avec un moteur d'évaluation d'expressions
et d'exécution de code Javascript (cf. JSR 223 pour qui est intéressé).
Il est donc possible d'utiliser Javascript dans les commandes, et
c'est le cas pour toutes les évaluations d'expression : commandes IF,
WHEN, WHILE et les substitutions par ^$[]."
pause

print "Il faut aussi savoir que le moteur d'évaluation est Javascript par
défaut, mais peut être changé par la commande ENGINE (voir l'aide de
la commande). Pour cela, il suffit de placer le fichier jar du moteur
dans le CLASSPATH, puis de taper (exemple avec JRuby) :
SQL> help engine
... (aide) ...
SQL> engine jruby"
pause

print "Cela dit, les scripts internes et les modules utilisent le
moteur Javascript, car il est par défaut et très satisfaisant.
Pour plus d'information, consultez la rubrique d'aide 'js-commands'."
pause

----------------------------------------------------------------------

-- Fin du didacticiel : au revoir et nettoyage
print "$<n>Nous voilà arrivés au terme de ce didacticiel sur l'utilisation
de la console LunaSQL. Nous avons découvert la console d'exécution de
commandes SQL et utilitaires internes.
Outre la syntaxe fun de LunaSQL, les variables et les substitutions,
nous avons vu les commandes SQL SELECT, UPDATE, INSERT, DELETE, CREATE,
ainsi que HELP, PRINT, INPUT, DEF, EXEC, SHELL, OPT, VERB, SPOOL, SHOW,
IMPORT, EXPORT, DISP, SIZE, SEEK, ARG, IF, WHEN, FOR, ENGINE, USE, mais
aussi defmacro et la déclaration et l'usage de macros.
Il reste bien d'autres commandes ou macros utiles pour la console, les
scripts ou l'usage avancé de LunaSQL, mais je vous laisse toutes les
découvrir à votre rythme par la commande HELP !

Pour terminer, faisons un peu de nettoyage :"
pause

print "SQL> DROP TABLE $(tb_commandes)"
DROP TABLE $(tb_commandes)
print "SQL> DROP TABLE $(tb_clients)"
DROP TABLE $(tb_clients)
print "SQL> DROP TABLE $(tb_produits)"
DROP TABLE $(tb_produits)
pause

print "Merci de votre attention, et très bonne utilisation de LunaSQL !"

sortie;
