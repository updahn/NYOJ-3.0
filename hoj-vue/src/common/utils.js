import Vue from 'vue';
import storage from '@/common/storage';
import { STORAGE_KEY, PROBLEM_LEVEL, FOCUS_MODE_ROUTE_NAME, PROBLEM_TYPE_LEVEL, PROBLEM_TYPE } from '@/common/constants';
import myMessage from '@/common/message';
import api from '@/common/api';
import store from '@/store';
import i18n from '@/i18n';
import JSZip from 'jszip';

function submissionMemoryFormat(a, b, type) {
  if (a === null || a === '' || a === undefined) return '--';
  if (a === 0) return '0 KB';
  var c = 1024,
    d = b || 1,
    e = ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
    f = Math.floor(Math.log(a) / Math.log(c));
  if (submissionIsAcmOrOi(type)) {
    return parseFloat((a / Math.pow(c, f)).toFixed(d)) + ' ' + e[f];
  } else {
    return '--';
  }
}

function submissionTimeFormat(time, type) {
  if (time === undefined || time === null || time === '') return '--';
  if (submissionIsAcmOrOi(type)) {
    return time + 'ms';
  } else {
    return '--';
  }
}

function submissionLengthFormat(length, type) {
  if (length === undefined || length === null || length === '') return '--';
  if (submissionIsAcmOrOi(type)) {
    return length + 'B';
  } else {
    return '--';
  }
}

function submissionLanguageFormat(language, type) {
  if (submissionIsAcmOrOi(type)) {
    return language;
  } else if (type === PROBLEM_TYPE.SELECT) {
    return i18n.t(`m.Selection`);
  } else if (type === PROBLEM_TYPE.FILL) {
    return i18n.t(`m.Filling`);
  } else if (type === PROBLEM_TYPE.DECIDED) {
    return i18n.t(`m.Decide`);
  }
}

function submissionIsAcmOrOi(type) {
  return type === null || type === '' || type === undefined || type === PROBLEM_TYPE.OI || type === PROBLEM_TYPE.ACM;
}

function getACRate(acCount, totalCount) {
  let rate = totalCount === 0 ? 0.0 : ((acCount / totalCount) * 100).toFixed(2);
  return String(rate) + '%';
}

// 去掉值为空的项，返回object
function filterEmptyValue(object) {
  let query = {};
  Object.keys(object).forEach((key) => {
    if (object[key] || object[key] === 0 || object[key] === false) {
      query[key] = object[key];
    }
  });
  return query;
}

// 按指定字符数截断添加换行，非英文字符按指定字符的半数截断
function breakLongWords(value, length = 16) {
  let re;
  if (escape(value).indexOf('%u') === -1) {
    // 没有中文
    re = new RegExp('(.{' + length + '})', 'g');
  } else {
    // 中文字符
    re = new RegExp('(.{' + (parseInt(length / 2) + 1) + '})', 'g');
  }
  return value.replace(re, '$1\n');
}

function downloadFile(url) {
  return new Promise((resolve, reject) => {
    Vue.prototype.$axios
      .get(url, { responseType: 'blob', timeout: 5 * 60 * 1000 })
      .then((resp) => {
        let headers = resp.headers;
        if (headers['content-type'].indexOf('json') !== -1) {
          let fr = new window.FileReader();
          if (resp.data.error) {
            myMessage.error(resp.data.error);
          }
          fr.onload = (event) => {
            let data = JSON.parse(event.target.result);
            if (data.msg) {
              myMessage.info(data.msg);
            } else {
              myMessage.error('Invalid file format');
            }
          };
          let b = new window.Blob([resp.data], { type: 'application/json' });
          fr.readAsText(b);
          return;
        }
        let link = document.createElement('a');
        link.href = window.URL.createObjectURL(new window.Blob([resp.data], { type: headers['content-type'] }));
        link.download = (headers['content-disposition'] || '').split('filename=')[1];
        document.body.appendChild(link);
        link.click();
        link.remove();
        myMessage.success('Downloading...');
        resolve();
      })
      .catch((error) => {
        reject(error);
      });
  });
}

function downloadBoxFile(url, fileName) {
  return new Promise((resolve, reject) => {
    Vue.prototype.$axios
      .get(url, {
        responseType: 'blob',
        timeout: 5 * 60 * 1000,
      })
      .then((resp) => {
        let headers = resp.headers;

        // 从URL中解析出文件类型
        let fileNameWithExtension = url.split('/').pop();
        let fileType = fileNameWithExtension.split('.').pop() || 'txt';
        let filename = fileName.split('.')[0];

        headers['content-disposition'] = `attachment; filename=${filename}.${fileType}`;
        headers['content-type'] = getContentType(fileType); // 获取 MIME 类型

        let link = document.createElement('a');
        link.href = window.URL.createObjectURL(new window.Blob([resp.data], { type: headers['content-type'] }));
        link.download = (headers['content-disposition'] || '').split('filename=')[1];
        document.body.appendChild(link);
        link.click();
        link.remove();
        myMessage.success('Downloading...');
        resolve();
      })
      .catch((error) => {
        reject(error);
      });
  });
}

function getContentType(fileType) {
  switch (fileType.toLowerCase()) {
    case 'txt':
      return 'text/plain';
    case 'pdf':
      return 'application/pdf';
    case 'doc':
    case 'docx':
      return 'application/msword';
    case 'xls':
    case 'xlsx':
      return 'application/vnd.ms-excel';
    case 'ppt':
    case 'pptx':
      return 'application/vnd.ms-powerpoint';
    case 'jpeg':
    case 'jpg':
      return 'image/jpeg';
    case 'png':
      return 'image/png';
    case 'gif':
      return 'image/gif';
    case 'mp3':
      return 'audio/mpeg';
    case 'mp4':
      return 'video/mp4';
    case 'zip':
      return 'application/zip';
    // 添加其他常见文件类型的映射...
    default:
      return 'application/octet-stream'; // 未知类型，默认为二进制流
  }
}

function downloadFileByText(fileName, fileContent) {
  return new Promise((resolve, reject) => {
    let link = document.createElement('a');
    link.href = window.URL.createObjectURL(new window.Blob([fileContent], { type: 'text/plain;charset=utf-8' }));
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    myMessage.success('Download Successfully!');
    resolve();
  });
}

async function readTestCase(problemID, name = null, fileListDir = null) {
  const query = [];
  if (problemID) query.push(`pid=${problemID}`);
  if (fileListDir) query.push(`fileListDir=${fileListDir}`);
  const url = `/api/file/download-testcase${query.length ? `?${query.join('&')}` : ''}`;

  let fileContents = {};
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Failed to fetch test case. Status: ${response.status}`);

    const blob = await response.blob();
    const zip = await JSZip.loadAsync(blob);

    const filesToRead = name ? [zip.file(name)] : Object.values(zip.files).filter((file) => !file.dir && file.name.includes('.'));
    const filePromises = filesToRead.map(async (file) => {
      const content = await file.async('text');
      const shortName = file.name.substring(file.name.lastIndexOf('/') + 1);
      fileContents[shortName] = content;
    });

    await Promise.all(filePromises);
  } catch (error) {
    fileContents = {};
    console.error('Error reading test case:', error.message);
  }
  return fileContents;
}

function getLanguages(all = true) {
  return new Promise((resolve, reject) => {
    let languages = storage.get(STORAGE_KEY.languages);
    if (languages) {
      resolve(languages);
    } else {
      api.getAllLanguages(all).then(
        (res) => {
          let langs = res.data.data;
          storage.set(STORAGE_KEY.languages, langs);
          resolve(langs);
        },
        (err) => {
          reject(err);
        }
      );
    }
  });
}

function stringToExamples(value) {
  let reg = '<input>([\\s\\S]*?)</input><output>([\\s\\S]*?)</output>';
  let re = RegExp(reg, 'g');
  let objList = [];
  let tmp;
  while ((tmp = re.exec(value))) {
    objList.push({ input: tmp[1], output: tmp[2] });
  }
  return objList;
}

function examplesToString(objList) {
  if (objList.length == 0) {
    return '';
  }
  let result = '';
  for (let obj of objList) {
    result += '<input>' + obj.input + '</input><output>' + obj.output + '</output>';
  }
  return result;
}

function getLevelColor(difficulty) {
  if (difficulty != undefined && difficulty != null) {
    if (PROBLEM_LEVEL[difficulty]) {
      return 'color: #fff !important;background-color:' + PROBLEM_LEVEL[difficulty]['color'] + ' !important;';
    } else {
      return 'color: #fff !important;background-color: rgb(255, 153, 0)!important;';
    }
  }
}
function getLevelName(difficulty) {
  if (difficulty != undefined && difficulty != null && PROBLEM_LEVEL[difficulty]) {
    return PROBLEM_LEVEL[difficulty]['name'][store.getters.webLanguage];
  } else {
    return 'unknown [' + difficulty + ']';
  }
}

function getTypeName(type) {
  if (type != undefined && type != null && PROBLEM_TYPE_LEVEL[type]) {
    return PROBLEM_TYPE_LEVEL[type]['name'][store.getters.webLanguage];
  } else {
    return 'unknown [' + type + ']';
  }
}

function getTypeColor(type) {
  if (type != undefined && type != null) {
    if (PROBLEM_TYPE_LEVEL[type]) {
      return 'color: #fff !important;background-color:' + PROBLEM_TYPE_LEVEL[type]['color'] + ' !important;';
    } else {
      return 'color: #fff !important;background-color: rgb(255, 153, 0)!important;';
    }
  }
}

function isFocusModePage(routeName) {
  for (let keyName in FOCUS_MODE_ROUTE_NAME) {
    if (keyName == routeName) {
      return true;
    }
  }
  return false;
}

function getFocusModeOriPage(routeName) {
  return FOCUS_MODE_ROUTE_NAME[routeName];
}

function supportFocusMode(routeName) {
  return routeName != 'ProblemDetails' && routeName != 'GroupProblemDetails';
}
function getSwitchFoceusModeRouteName(routeName) {
  for (let keyName in FOCUS_MODE_ROUTE_NAME) {
    if (keyName == routeName) {
      return FOCUS_MODE_ROUTE_NAME[keyName];
    } else if (FOCUS_MODE_ROUTE_NAME[keyName] == routeName) {
      return keyName;
    }
  }
}
function getRouteRealName(routeName, contestID, trainingID, groupID, routeType) {
  const isFullScreen = routeName.includes('full-screen');
  const isExam = routeName.includes('exam');
  const groupPrefix = groupID ? 'Group' : '';
  const fullPrefix = isFullScreen ? 'Full' : '';

  if (routeType == 'SubmissionList' || routeType == 'SubmissionDetails') {
    if (trainingID) return fullPrefix ? `${groupPrefix}Training${fullPrefix}${routeType}` : `${groupPrefix}${fullPrefix}${routeType}`;
  }

  if (contestID) {
    if (isExam) {
      return `${groupPrefix}Exam${fullPrefix}${routeType}`;
    } else {
      return `${groupPrefix}Contest${fullPrefix}${routeType}`;
    }
  }
  if (trainingID) return `${groupPrefix}Training${fullPrefix}${routeType}`;

  return `${groupPrefix}${fullPrefix}${routeType}`;
}

function getValidateField(field, fieldName) {
  if (!field) {
    myMessage.error(i18n.t(`m.${fieldName}`) + ' ' + i18n.t('m.is_required'));
    return true;
  } else {
    if (field.indexOf('$') !== -1) {
      myMessage.error(i18n.t(`m.${fieldName}`) + ' ' + i18n.t('m.The_title_role'));
      return true;
    }
  }
  return false;
}

export default {
  submissionMemoryFormat: submissionMemoryFormat,
  submissionTimeFormat: submissionTimeFormat,
  submissionLengthFormat: submissionLengthFormat,
  submissionLanguageFormat: submissionLanguageFormat,
  submissionIsAcmOrOi: submissionIsAcmOrOi,
  getACRate: getACRate,
  filterEmptyValue: filterEmptyValue,
  breakLongWords: breakLongWords,
  downloadFile: downloadFile,
  downloadBoxFile: downloadBoxFile,
  downloadFileByText: downloadFileByText,
  getLanguages: getLanguages,
  stringToExamples: stringToExamples,
  examplesToString: examplesToString,
  getLevelColor: getLevelColor,
  getLevelName: getLevelName,
  getTypeName: getTypeName,
  getTypeColor: getTypeColor,
  isFocusModePage: isFocusModePage,
  getFocusModeOriPage: getFocusModeOriPage,
  supportFocusMode: supportFocusMode,
  getSwitchFoceusModeRouteName: getSwitchFoceusModeRouteName,
  getRouteRealName: getRouteRealName,
  getValidateField: getValidateField,
  readTestCase: readTestCase,
};
