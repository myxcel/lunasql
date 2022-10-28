/* *************
 * INIT et TESTS
 * *************/
--
need 4.8

def tests-unit exec "LunaSQL-4.9/misc/tests-unit.sql"
def tests-perf exec "LunaSQL-4.9/misc/tests-perf.sql"
def pok,pko {print ok} {print ko}

opt :ON_INIT {print "Bonjour"}
opt :ON_QUIT {print "Au revoir"}

return



/* $$ BEGIN SIGNATURE $$
DJk6bXiYW92tz1vhCMG+iAuVGQKs3KntVsLRfyWgh8IBeJg6fTTFQNIr5T3YzaIaNvAc
zEyBo/Had//OYoyiGIgzCR5mvXDoOepRuKZLnYAV6elNcJnxoEDDp4gFBOcJLO5MbjQA
 * $$ END SIGNATURE $$ */
