(function() {
  const cssList = [
    'https://cdn.jsdelivr.net/npm/element-ui@2.15.3/lib/theme-chalk/index.min.css',
    'https://cdn.jsdelivr.net/npm/github-markdown-css@4.0.0/github-markdown.min.css',
    'https://cdn.jsdelivr.net/npm/katex@0.12.0/dist/katex.min.css',
    'https://cdn.jsdelivr.net/npm/muse-ui@3.0.2/dist/muse-ui.min.css',
    'https://cdn.jsdelivr.net/npm/font-awesome@4.7.0/css/font-awesome.min.css',
    'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css'
  ];
  const jsList = [
    'https://cdn.jsdelivr.net/npm/vue@2.6.11/dist/vue.min.js',
    'https://cdn.jsdelivr.net/npm/vue-router@3.2.0/dist/vue-router.min.js',
    'https://cdn.jsdelivr.net/npm/vuex@3.4.0/dist/vuex.min.js',
    'https://cdn.jsdelivr.net/npm/axios@0.21.0/dist/axios.min.js',
    'https://cdn.jsdelivr.net/npm/echarts@4.9.0/dist/echarts.min.js',
    'https://cdn.jsdelivr.net/npm/highlight.js@10.3.2/lib/highlight.min.js',
    'https://cdn.jsdelivr.net/npm/moment@2.29.1/min/moment.min.js',
    'https://cdn.jsdelivr.net/npm/moment@2.29.1/locale/zh-cn.js',
    'https://cdn.jsdelivr.net/npm/moment@2.29.1/locale/en-gb.js',
    'https://cdn.jsdelivr.net/npm/element-ui@2.15.3/lib/index.js',
    'https://cdn.jsdelivr.net/npm/muse-ui@3.0.2/dist/muse-ui.min.js',
    'https://cdn.jsdelivr.net/npm/jquery@3.5.1/dist/jquery.min.js',
    'https://cdn.jsdelivr.net/npm/crypto-js@4.2.0/crypto-js.min.js',
    'https://cdn.jsdelivr.net/npm/jszip@3.10.1/dist/jszip.min.js',
    'https://cdn.jsdelivr.net/npm/file-saver@2.0.5/dist/FileSaver.min.js',
    'https://cdn.jsdelivr.net/npm/vue-echarts@5.0.0-beta.0/dist/vue-echarts.min.js',
    'https://cdn.jsdelivr.net/npm/xlsx@0.18.5/dist/xlsx.full.min.js',
    'https://cdn.jsdelivr.net/npm/vue-i18n@8.24.4/dist/vue-i18n.min.js',
    'https://cdn.jsdelivr.net/npm/papaparse@5.3.0/papaparse.min.js',
    'https://cdn.jsdelivr.net/npm/nprogress@0.2.0/nprogress.min.js',
    'https://cdn.jsdelivr.net/npm/vue-avatar@2.3.3/dist/vue-avatar.min.js',
    'https://cdn.jsdelivr.net/npm/mavon-editor@2.9.1/dist/mavon-editor.min.js',
    'https://cdn.jsdelivr.net/npm/vue-cropper@0.5.5/dist/index.js',
    'https://cdn.jsdelivr.net/npm/darkreader@4.9.40/darkreader.min.js',
  ];

  const findComment = (text) => {
    const walker = document.createTreeWalker(document, NodeFilter.SHOW_COMMENT);
    let node;
    while ((node = walker.nextNode())) {
      if (node.nodeValue.trim() === text) return node;
    }
  };

  const inject = () => {
    const cssTarget = findComment('使用CDN的CSS文件');
    const jsTarget = findComment('使用CDN的JS文件');
    if (!cssTarget || !jsTarget) {
      return;
    }
    cssList.forEach((href) => {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = href;
      cssTarget.parentNode.insertBefore(link, cssTarget.nextSibling);
    });
    jsList.forEach((src) => {
      const script = document.createElement('script');
      script.src = src;
      script.async = false;
      jsTarget.parentNode.insertBefore(script, jsTarget.nextSibling);
    });
  };

  const checkNetwork = (cb) => {
    const img = new Image();
    let finished = false;
    const timeout = setTimeout(() => {
      if (!finished) {
        finished = true;
        cb(false);
      }
    }, 2000); // 2秒超时
    img.onload = () => {
      if (!finished) {
        clearTimeout(timeout);
        finished = true;
        cb(true);
      }
    };
    img.onerror = () => {
      if (!finished) {
        clearTimeout(timeout);
        finished = true;
        cb(false);
      }
    };
    img.src = 'https://www.baidu.com/favicon.ico?_t=' + Date.now();
  };

  const start = () => {
    checkNetwork((ok) => {
      if (ok) inject();
    });
  };

  document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', start) : start();
})();
