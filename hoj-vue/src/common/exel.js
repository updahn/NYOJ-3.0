import { Buffer } from 'buffer';
import Papa from 'papaparse';
import * as XLSX from 'xlsx';
import jschardet from 'jschardet';
import iconv from 'iconv-lite';

class FileParserError extends Error {
  constructor(message, fileType, cause) {
    super(`${message} (FileType: ${fileType})`);
    this.name = 'FileParserError';
    this.fileType = fileType;
    this.cause = cause;
  }
}

class FileParser {
  constructor() {
    this.fullData = [];
  }

  async parse(file, currentPage, pageSize) {
    const fileType = file.name
      .split('.')
      .pop()
      .toLowerCase();

    try {
      this.fullData = [];

      if (fileType === 'csv') {
        await this.parseCSV(file);
      } else if (['xlsx', 'xls'].includes(fileType)) {
        await this.parseExcel(file);
      } else {
        throw new FileParserError('不支持的文件类型', fileType);
      }

      return this.handleResult(currentPage, pageSize);
    } catch (error) {
      if (!(error instanceof FileParserError)) {
        throw new FileParserError('文件解析失败', fileType, error);
      }
      throw error;
    }
  }

  handleResult(currentPage, pageSize) {
    try {
      if (currentPage === undefined || pageSize === undefined) {
        return this.fullData;
      }

      if (typeof currentPage !== 'number' || typeof pageSize !== 'number') {
        throw new Error('分页参数必须为数字');
      }

      return this.getPaginatedData(currentPage, pageSize);
    } catch (error) {
      throw new FileParserError('数据处理失败', 'N/A', error);
    }
  }

  formatRowData(row) {
    return Array.isArray(row) ? row.map((cell) => String(cell || '')) : String(row || '');
  }

  async parseCSV(file) {
    try {
      const buffer = await this.readFileAsBuffer(file);
      const encoding = this.detectEncoding(buffer);
      const content = this.decodeContent(buffer, encoding);

      await new Promise((resolve, reject) => {
        Papa.parse(content, {
          header: false,
          dynamicTyping: true,
          complete: (results) => {
            this.fullData = results.data.map((row) => this.formatRowData(row));
            resolve();
          },
          error: (error) => reject(new Error(`CSV解析错误: ${error.message}`)),
        });
      });
    } catch (error) {
      throw new FileParserError('CSV文件解析失败', 'csv', error);
    }
  }

  async parseExcel(file) {
    try {
      const buffer = await this.readFileAsBuffer(file);
      const workbook = XLSX.read(buffer, { type: 'buffer' });

      if (workbook.SheetNames.length === 0) {
        throw new Error('Excel文件中未找到工作表');
      }

      const worksheet = workbook.Sheets[workbook.SheetNames];
      const data = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
      this.fullData = data.map((row) => this.formatRowData(row));
    } catch (error) {
      throw new FileParserError('Excel文件解析失败', 'excel', error);
    }
  }

  getPaginatedData(currentPage, pageSize) {
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;

    if (start > this.fullData.length) {
      throw new Error(`请求页码超出范围 (总数据量: ${this.fullData.length})`);
    }

    return this.fullData.slice(start, end);
  }

  readFileAsBuffer(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = () => reject(new Error('文件读取失败'));
      reader.readAsArrayBuffer(file);
    });
  }

  detectEncoding(buffer) {
    try {
      const detection = jschardet.detect(Buffer.from(buffer));
      if (!detection.encoding) {
        throw new Error('无法检测文件编码');
      }
      return detection.encoding;
    } catch (error) {
      throw new Error(`编码检测失败: ${error.message}`);
    }
  }

  decodeContent(buffer, encoding) {
    try {
      return encoding === 'UTF-8' ? new TextDecoder('utf-8').decode(buffer) : iconv.decode(Buffer.from(buffer), encoding);
    } catch (error) {
      throw new Error(`内容解码失败: ${error.message}`);
    }
  }
}

export const exel = new FileParser();
