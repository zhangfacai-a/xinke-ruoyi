import request from '@/utils/request'

export function listExpense(query) {
  return request({ url: '/finance/expense/list', method: 'get', params: query })
}

export function addExpense(data) {
  return request({ url: '/finance/expense', method: 'post', data })
}

export function approveExpense(expenseNo) {
  return request({ url: `/finance/expense/${expenseNo}/approve`, method: 'post' })
}

export function createExpenseVoucher(expenseNo) {
  return request({ url: `/finance/expense/${expenseNo}/voucher`, method: 'post' })
}
