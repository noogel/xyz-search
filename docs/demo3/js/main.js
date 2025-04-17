/**
 * 智能检索平台 - 主要JavaScript函数
 */

// 通用工具函数
const RAGApp = {
  /**
   * 简化DOM元素选择
   */
  $(selector) {
    return document.querySelector(selector);
  },
  
  $$(selector) {
    return document.querySelectorAll(selector);
  },
  
  /**
   * 显示加载状态
   */
  showLoading(element) {
    if (!element) return;
    
    element.innerHTML = `
      <div class="flex justify-center items-center py-8">
        <svg class="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <span class="ml-3 text-gray-600">正在加载...</span>
      </div>
    `;
  },
  
  /**
   * 隐藏加载状态
   */
  hideLoading(element) {
    if (!element) return;
    element.innerHTML = '';
  },
  
  /**
   * 显示通知消息
   */
  showNotification(message, type = 'success') {
    const notificationContainer = this.$('#notification-container');
    
    if (!notificationContainer) {
      const container = document.createElement('div');
      container.id = 'notification-container';
      container.className = 'fixed top-5 right-5 z-50 flex flex-col space-y-2';
      document.body.appendChild(container);
    }
    
    const notification = document.createElement('div');
    const typeClasses = {
      'success': 'bg-green-500',
      'error': 'bg-red-500',
      'info': 'bg-blue-500',
      'warning': 'bg-yellow-500'
    };
    
    notification.className = `${typeClasses[type] || 'bg-gray-800'} text-white py-2 px-4 rounded shadow-lg flex items-center transition transform`;
    notification.innerHTML = `
      <div class="mr-2">
        ${type === 'success' ? '<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>' : ''}
        ${type === 'error' ? '<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>' : ''}
        ${type === 'info' ? '<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>' : ''}
        ${type === 'warning' ? '<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>' : ''}
      </div>
      <div>${message}</div>
    `;
    
    this.$('#notification-container').appendChild(notification);
    
    // 动画效果
    setTimeout(() => {
      notification.classList.add('opacity-0');
      setTimeout(() => {
        notification.remove();
      }, 300);
    }, 3000);
  },
  
  /**
   * 模拟API请求
   */
  async simulateAPIRequest(endpoint, data = {}, delay = 1000) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          data
        });
      }, delay);
    });
  },
  
  /**
   * 格式化日期
   */
  formatDate(date) {
    const d = new Date(date);
    return d.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  },
  
  /**
   * 文件大小格式化
   */
  formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  },
  
  /**
   * 获取文件扩展名
   */
  getFileExtension(filename) {
    return filename.slice((filename.lastIndexOf(".") - 1 >>> 0) + 2);
  },
  
  /**
   * 获取文件类型图标
   */
  getFileIcon(filename) {
    const ext = this.getFileExtension(filename).toLowerCase();
    
    const iconMap = {
      'pdf': '<svg class="h-6 w-6 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path></svg>',
      'doc': '<svg class="h-6 w-6 text-blue-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path></svg>',
      'docx': '<svg class="h-6 w-6 text-blue-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path></svg>',
      'xls': '<svg class="h-6 w-6 text-green-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path></svg>',
      'xlsx': '<svg class="h-6 w-6 text-green-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path></svg>',
      'txt': '<svg class="h-6 w-6 text-gray-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path></svg>',
      'jpg': '<svg class="h-6 w-6 text-pink-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd"></path></svg>',
      'jpeg': '<svg class="h-6 w-6 text-pink-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd"></path></svg>',
      'png': '<svg class="h-6 w-6 text-pink-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd"></path></svg>',
      'zip': '<svg class="h-6 w-6 text-yellow-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path><path d="M10 4h2v2h-2V4zM10 8h2v2h-2V8zM10 12h2v2h-2v-2z"></path></svg>',
      'rar': '<svg class="h-6 w-6 text-yellow-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path><path d="M10 4h2v2h-2V4zM10 8h2v2h-2V8zM10 12h2v2h-2v-2z"></path></svg>',
    };
    
    return iconMap[ext] || '<svg class="h-6 w-6 text-gray-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path></svg>';
  },
  
  /**
   * 深色模式切换
   */
  initDarkMode() {
    const darkModeToggle = this.$('#dark-mode-toggle');
    if (!darkModeToggle) return;
    
    const isDarkMode = localStorage.getItem('dark-mode') === 'true';
    
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    }
    
    darkModeToggle.addEventListener('click', () => {
      document.documentElement.classList.toggle('dark');
      
      const isDarkModeNow = document.documentElement.classList.contains('dark');
      localStorage.setItem('dark-mode', isDarkModeNow);
    });
  },
  
  /**
   * 初始化页面
   */
  init() {
    this.initDarkMode();
    
    // 自动设置页面内容区域最小高度，使页脚始终在底部
    const header = this.$('header');
    const footer = this.$('footer');
    const main = this.$('main');
    
    if (header && footer && main) {
      const setMainHeight = () => {
        const headerHeight = header.offsetHeight;
        const footerHeight = footer.offsetHeight;
        const windowHeight = window.innerHeight;
        
        main.style.minHeight = `${windowHeight - headerHeight - footerHeight}px`;
      };
      
      setMainHeight();
      window.addEventListener('resize', setMainHeight);
    }
  }
};

// 当文档加载完成时初始化
document.addEventListener('DOMContentLoaded', () => {
  RAGApp.init();
}); 