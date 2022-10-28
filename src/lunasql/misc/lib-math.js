/**
 * Script de fonctions mathématiques pour le moteur d'évaluation JavaScript de LunaSQL
 */

// Raccourcis des fonctions usuelles
var E=Math.E,PI=Math.PI,abs=Math.abs,max=Math.max,min=Math.min,sin=Math.sin,cos=Math.cos,tan=Math.tan,asin=Math.asin,acos=Math.acos,atan=Math.atan,atan2=Math.atan2,exp=Math.exp,ln=Math.log,log=Math.log10,sqrt=Math.sqrt,cbrt=Math.cbrt,ceil=Math.ceil,floor=Math.floor,pow=Math.pow,round=Math.round,rand=Math.random;
function randn(n){if(arguments.length==0)n=100;return Math.floor(Math.random()*n);}

/* Created 1997 by Brian Risk.  http://brianrisk.com - licence : public domain -- edited by M.P.
 * http://www.geneffects.com/briarskin/programming/newJSMathFuncs.html
 */
/* Convert a number with base 36 or less to any other base of 36 or less. ex. intToBase('46406810206',10,16); */
function intToBase(number,ob,nb){number=number.toUpperCase();var list="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";var dec=0;for(var i=0;i<=number.length;i++){dec+=(list.indexOf(number.charAt(i)))*(Math.pow(ob,(number.length-i-1)));} number="";var magnitude=Math.floor((Math.log(dec))/(Math.log(nb)));for(var i=magnitude;i>=0;i--){var amount=Math.floor(dec/Math.pow(nb,i));number=number+list.charAt(amount);dec -= amount*(Math.pow(nb,i));} return number;}
/* Compute logarithms with bases other than the standard e or 10. ex. logb(16,2); */
function logb(x,base){return (Math.log(x))/(Math.log(base));}
/* Rounds to user specified number of decimal places. ex. roundp(3.14159265,4); */
function roundp(x,places){if(places>10)places=10;return (Math.round(x*Math.pow(10,places)))/Math.pow(10,places);}
/* Give a denominator limit, and it will return the best fraction equivalent to a given decimal (up to that denominator limit.) ex. fractApprox(1.58496,16); */
function fractApprox(x,maxDenominator){maxDenominator=parseInt(maxDenominator);var approx=0;var error=0;var best=0;var besterror=0;for(var i=1;i<=maxDenominator;i++){approx=Math.round(x/(1/i));error=(x-(approx/i));if(i==1){best=i;besterror=error;} if(Math.abs(error)<Math.abs(besterror)){best=i;besterror=error;}} return (Math.round(x/(1/best))+"/"+best);}

/* Reduce a fraction by finding the Greatest Common Divisor and dividing by it. ex. fractReduce(13427,3413358); returns [463,117702]
 * Source : http://stackoverflow.com/questions/4652468/is-there-a-javascript-function-that-reduces-a-fraction */
function fractReduce(numerator,denominator){var gcd=function gcd(a,b){return b?gcd(b,a%b):a;};gcd=gcd(numerator,denominator);return [numerator/gcd,denominator/gcd];}
