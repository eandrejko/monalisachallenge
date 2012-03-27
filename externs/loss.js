goog.provide('monalisachallenge.loss')

monalisachallenge.loss.l1 = function(a,b){
  var s = 0;
  var l = a.length;
  for(i = 0; i < l; i++){
    s = s + Math.abs(a[i] - b[i]);
  }
  return s;
}

monalisachallenge.loss.l2 = function(a,b){
  var s = 0;
  var l = a.length;
  for(i = 0; i < l; i++){
    s = s + (a[i] - b[i])*(a[i] - b[i]);
  }
  return s;
}