/**
 * Script d'initialisation du contexte pour le moteur d'évaluation JavaScript
 * Exécuté au démarrage de LunaSQL
 */

/*
 * Détection de types d'objets et adresses
 */
// True et False
var TRUE = true, FALSE = false, NULL = null;
// Chargement d'un fichier JavaScript
function include(file) {
   if (typeof script_engine === 'undefined') throw 'Objet script_engine non disponible';
   script_engine.eval(new java.io.InputStreamReader(new java.io.FileInputStream(file)));
}
// Is an object a string
function isString(obj){return typeof obj==='string';}
// Is an object a array
function isArray(obj){return obj && !(obj.propertyIsEnumerable('length')) && typeof obj==='object' && typeof obj.length==='number';}
// Is an object a int
function isInt(obj){var re=/^[+-]?\d+$/;return re.test(obj);}
//Is an object a float
function isFloat(obj){var re=/^[+-]?\d+[.]\d+((E|e)[+-]?\d+)?$/;return re.test(obj);}
// Is an object a email address
function isEmail(obj){if(isString(obj)){return obj.match(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b/ig);}else{return false;}}
// Is an object a URL
function isUrl(obj){if(isString(obj)){var re=new RegExp("^(http|https)\://([a-zA-Z0-9\.\-]+(\:[a-zA-Z0-9\.&%\$\-]+)*@)*((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|localhost|([a-zA-Z0-9\-]+\.)*[a-zA-Z0-9\-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(\:[0-9]+)*(/($|[a-zA-Z0-9\.\,\?\'\\\+&%\$#\=~_\-]+))*$");return obj.match(re);}else{return false;}}
// Formate en supprimant les décimales
function I(n){return n.toFixed(0);}

/**
 * Fonctions de manipulation de strings pour le moteur d'évaluation JavaScript de LunaSQL
 */
function trim(s){return s.replace(/^\s+|\s+$/g,'');}
function ltrim(s){return s.replace(/^\s+/,'');}
function rtrim(s){return s.replace(/\s+$/,'');}
function fulltrim(s){return s.replace(/(?:(?:^|\n)\s+|\s+(?:$|\n))/g,'').replace(/\s+/g,' ');}
function truncate(s,len){if (s.length > len)s = s.substring(0, len); return str;}
function onlyLetters(s){return s.toLowerCase().replace(/[^a-z]/g, '');}
function onlyLettersNums(s){return s.toLowerCase().replace(/[^a-z,0-9,-]/g,'');}
function ends(s,r){return s.match(r+'$')==r;}
function starts(s,r) {return s.match('^'+r)==r;}
function upper(s){return s.toUpperCase();}
function lower(s){return s.toLowerCase();}
function lpad(s,pad,len){while(s.length<len)s=pad+s;return s;}
function rpad(s,pad,len){while(s.length<len)s=s+pad;return s;}
function stripHtml(s){return s.replace(/<([^>]+)>/g, '');}
function encodeHtml(s){return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}

/**
 * Script de fonctions utilitaires de tableaux pour le moteur d'évaluation JavaScript de LunaSQL
 * Source : http://4umi.com/web/javascript/array.php
 */
var sortn=function(arr){return arr.sort(function (a,b){return a-b;});}
var find=function(arr,s){var r=false;for(i=0; i<arr.length; i++) {if(typeof(s)==='function') {if(s.test(arr[i])) {if(!r)r=[];r.push(i);}}else if (arr[i] === s) {if(!r)r=[];r.push(i);}}return r;}
var map=function(arr,f){var r=[];for (i=0;i<arr.length;i++)r.push(f(arr[i]));return r;}
var indexOf=function(arr,n){for(var i=0; i<arr.length;i++) if(arr[i]===n) return i;return -1;}
var lastIndexOf=function(arr,n){var i=arr.length;while(i--){if(arr[i]===n){return i;}}return -1;}
var forEach=function(arr,f){var i=arr.length,j,l=arr.length;for(i=0;i<l;i++){if((j=arr[i])){f(j);}}}
var insert=function(arr,i,v){if(i>=0){var a=arr.slice(),b=a.splice(i);a[i]=value;return a.concat(b);}}
var shuffle=function(arr){var i=arr.length,j,t;while(i--){j=Math.floor((i+1)*Math.random());t=arr[i];arr[i]=arr[j];arr[j]=t;}}
var unique=function(arr){var a=[],i;arr.sort();for(i=0;i<arr.length;i++){if(arr[i]!==arr[i+1]){a[a.length]=arr[i];}}return a;}
var concat=function(arr,a){for(var i=0,b=arr.copy();i<a.length;i++){b[b.length]=a[i];}return b;}
var copy=function(arr,a){var a=[],i=arr.length;while(i--){a[i]=(typeof arr[i].copy!=='undefined')?arr[i].copy():arr[i];}return a;}
var pop=function(arr){var b=arr[arr.length-1];arr.length--;return b;}
var push=function(arr){for(var i=0,b=arr.length,a=arguments;i<a.length;i++){arr[b+i]=a[i];}return arr.length;}
var shift=function(arr){for(var i=0,b=arr[0];i<arr.length-1;i++){arr[i]=arr[i+1];}arr.length--;return b;}
var slice=function(arr,a,c){var i=0,b,d=[];if(!c){c=arr.length;}if(c<0){c=arr.length+c;}if(a<0){a=arr.length-a;}if(c<a){b=a;a=c;c=b;}for(i;i<c-a;i++){d[i]=arr[a+i];}return d;}
var splice=function(arr,a,c){var i=0,e=arguments,d=arr.copy(),f=a;if(!c){c=arr.length-a;}for(i;i<e.length-2;i++){arr[a+i]=e[i+2];}for(a;a<arr.length-c;a++){arr[a+e.length-2]=d[a-c];}arr.length-=c-e.length+2;return d.slice(f,f+c);}
var unshift=function(arr,a){arr.reverse();var b=arr.push(a);arr.reverse();return b;}
var remove=function(arr,v){var i=0;while(i<arr.length){if(arr[i]===v){arr.splice(i,1);}else{i++;}}}
var contains=function(arr,v){var i=arr.length;while (i--){if(arr[i]===v){return true;}}return false;}
/* Sorts a array of objects by a property */
function sortObjects(field,reverse,primer){reverse=(reverse)?-1:1;return function(a,b){a=a[field];b=b[field];if(primer!==undefined && a!==undefined && b!==undefined){a=primer(a);b=primer(b);} if(a<b)return reverse*-1;if(a>b)return reverse*1;return 0;}}
function sortObjectsByProperty(arr,field,reverse,primer) {return arr.sort(sortObjects(field,reverse,primer));}

/**
 * Fonctions de date
 */
function millis() {return Packages.java.lang.System.currentTimeMillis();}
function formatDate(format, date) {
  if (!format) format = "yyyy-MM-dd HH:mm:ss";
  var d = date || new Date();
  var o = {
    "M+" : d.getMonth()+1, //month
    "d+" : d.getDate(),    //day
    "H+" : d.getHours(),   //hour
    "m+" : d.getMinutes(), //minute
    "s+" : d.getSeconds(), //second
    "q+" : Math.floor((d.getMonth()+3)/3),  //quarter
    "S" : d.getMilliseconds() //millisecond
  };
  if(/(y+)/.test(format))
    format = format.replace(RegExp.$1, (d.getFullYear()+"").substr(4 - RegExp.$1.length));
  for(var k in o)
    if (new RegExp("("+ k +")").test(format))
      format = format.replace(RegExp.$1,
        RegExp.$1.length==1 ? o[k] : ("00"+ o[k]).substr((""+ o[k]).length));
  return format;
}

/**
 * Gestion du format JSON
 */
// objectToJson(obj [,sp]) utilise JSON.stringify(value [, replacer] [, space])
function objectToJson(obj,sp){if(typeof sp==="undefined")sp=true; return JSON.stringify(obj,null,sp?2:0);}
//toObject(str) utilise JSON.parse(text [, reviver])
function jsonToObject(str){return JSON.parse(str);}
// Impression d'un object par JSON
function printObject(obj){print(toJSON(obj,true));}

/**
 * Un peu d'affichage graphique
 */
/* Message d'information */
function messageInfo(m, t) {
   Packages.javax.swing.JOptionPane.showMessageDialog(null, m, t, Packages.javax.swing.JOptionPane.INFORMATION_MESSAGE);
}
/* Message d'avertissement */
function messageWarn(m, t) {
   Packages.javax.swing.JOptionPane.showMessageDialog(null, m, t, Packages.javax.swing.JOptionPane.WARNING_MESSAGE);
}
/* Message d'erreur application */
function messageError(m, t) {
   Packages.javax.swing.JOptionPane.showMessageDialog(null, m, t, Packages.javax.swing.JOptionPane.ERROR_MESSAGE);
}

