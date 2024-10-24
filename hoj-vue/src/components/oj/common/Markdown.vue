<template>
  <div v-if="isAvoidXss" v-dompurify-html="html" v-highlight v-katex class="markdown-body"></div>
  <div v-else v-html="html" v-highlight v-katex class="markdown-body"></div>
</template>
<script>
export default {
  name: "Markdown",
  props: {
    isAvoidXss: {
      default: false,
      type: Boolean,
    },
    content: {
      require: true,
      type: String,
    },
  },
  data() {
    return {
      pdfLogo: require("@/assets/pdf-logo.svg"),
    };
  },
  computed: {
    html: function () {
      // 如果内容为空，直接返回空字符串
      if (this.content == null || this.content == undefined) {
        return "";
      }

      // 渲染 markdown 内容
      let res = this.$markDown.render(this.content);

      // 定义一个检查 PDF 链接是否有效的函数
      function checkPDF(url) {
        let xhr = new XMLHttpRequest();
        xhr.open("HEAD", url, false); // 使用同步的 HEAD 请求检查链接
        try {
          xhr.send();
          // 如果状态码为 404，则 PDF 文件不存在
          if (xhr.status === 404) {
            return false;
          }
          // 其他状态码则认为文件存在
          return true;
        } catch (error) {
          // 如果发生错误，也返回 false 表示链接不可用
          return false;
        }
      }

      // 匹配并替换 markdown 中的 PDF 链接
      res = res.replace(
        /<a.*?href="(.*?.pdf)".*?>(.*?)<\/a>/gi,
        (match, pdfUrl, pdfName) => {
          // 使用 checkPDF 函数同步检查该 PDF 链接是否存在
          const exists = checkPDF(pdfUrl);

          // 如果 PDF 存在，返回正常的 PDF 预览模块
          if (exists) {
            return `
          <p></p>
          <file-card>
              <div>
                  <img class="pdf-svg" src="${this.pdfLogo}">
              </div>
              <div>
                  <h5 class="filename">${pdfName}</h5>
                  <p><a href="${pdfUrl}" target="_blank">Download</a></p>
              </div>
          </file-card>
          <object data="${pdfUrl}" type="application/pdf" width="100%" height="800px">
              <embed src="${pdfUrl}">
              浏览器不支持 PDF 预览，请下载 PDF 文件查看：<a href="${pdfUrl}" target="_blank">下载 PDF</a>。
              </embed>
          </object>
        `;
          } else {
            // 如果 PDF 不存在，返回 "Whitelabel Error Page" 错误提示
            return `题面已失效，请等待网站管理更新题面
                  <h4>There was an unexpected error (type=Not Found, status=404).</h4>`;
          }
        }
      );

      // 返回处理后的内容
      return res;
    },
  },
};
</script>
<style>
file-card .pdf-svg {
  padding: 0 !important;
  margin: 0 !important;
  box-shadow: none !important;
}
file-card {
  margin: 1rem 0;
  display: flex;
  align-items: center;
  max-width: 100%;
  border-radius: 4px;
  transition: 0.2s ease-out 0s;
  color: #7a8e97;
  background: #fff;
  padding: 0.6rem;
  position: relative;
  border: 1px solid rgba(0, 0, 0, 0.15);
}
file-card > div:first-of-type {
  display: flex;
  align-items: center;
  padding-right: 1rem;
  width: 5rem;
  height: 5rem;
  flex-shrink: 0;
  flex-grow: 0;
}
file-card .filename {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 1.2rem;
  margin-bottom: 0.5rem !important;
  font-family: "Roboto";
  font-weight: 400 !important;
  line-height: 1.2 !important;
  color: #000;
  word-wrap: break-word;
  word-break: break-all;
  white-space: normal !important;
  -webkit-line-clamp: 1;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}
file-card p {
  margin: 0;
  line-height: 1;
  font-family: "Roboto";
}
</style>