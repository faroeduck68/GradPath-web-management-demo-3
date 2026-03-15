import type { RouteRecordRaw } from 'vue-router'
import AdminLayout from '@/layouts/admin/AdminLayout.vue'

const adminRoutes: RouteRecordRaw = {
  path: '/admin',
  component: AdminLayout,
  redirect: '/admin/dashboard',
  children: [
    {
      path: 'dashboard',
      name: 'admin-dashboard',
      component: () => import('@/views/admin/DashboardView.vue'),
      meta: { title: '数据大盘' },
    },
    {
      path: 'jobs/spider',
      name: 'admin-jobs-spider',
      component: () => import('@/views/admin/JobsSpiderView.vue'),
      meta: { title: '岗位爬虫控制台' },
    },
    {
      path: 'jobs/review',
      name: 'admin-jobs-review',
      component: () => import('@/views/admin/JobsReviewView.vue'),
      meta: { title: '面经与岗位审核' },
    },
    {
      path: 'algorithm/questions',
      name: 'admin-algorithm-questions',
      component: () => import('@/views/admin/AlgorithmQuestionsView.vue'),
      meta: { title: '算法题库管理' },
    },
    {
      path: 'interview/monitor',
      name: 'admin-interview-monitor',
      component: () => import('@/views/admin/InterviewMonitorView.vue'),
      meta: { title: 'AI 面试监控' },
    },
  ],
}

export default adminRoutes
