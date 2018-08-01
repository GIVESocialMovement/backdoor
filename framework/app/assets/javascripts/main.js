window.parse = function(base64encoded) {
  return JSON.parse(decodeURIComponent(escape(atob(base64encoded))));
};

var tagsToReplace = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;'
};

window.replaceTag = function(tag) {
  return tagsToReplace[tag] || tag;
};

window.formatTime = (seconds) => {
  if (!seconds) { return ''; }

  let options = { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true };

  if (seconds && seconds > 0) {
    return new Date(seconds * 1000).toLocaleString('en-GB', options);
  } else {
    return "";
  }
};

if (window.Vue) {
  Vue.prototype.$formatTime = window.formatTime;
}
