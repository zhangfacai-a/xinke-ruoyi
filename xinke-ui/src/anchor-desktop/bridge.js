let next=0
const waiting=new Map(),listeners=new Set()
window.chrome?.webview?.addEventListener('message',event=>{
  const value=event.data
  if(value?.event==='state'){for(const listener of listeners)listener(value.data);return}
  const task=waiting.get(value?.id);if(!task)return
  waiting.delete(value.id);clearTimeout(task.timer)
  value.error?task.reject(new Error(value.error)):task.resolve(value.data)
})
export function native(command,data={}){
  return new Promise((resolve,reject)=>{
    if(!window.chrome?.webview){reject(new Error('请在鑫客主播工作台App内使用'));return}
    const id=++next,timer=setTimeout(()=>{waiting.delete(id);reject(new Error('处理超时，请查看日志并重试'))},90000)
    waiting.set(id,{resolve,reject,timer});window.chrome.webview.postMessage({id,command,data})
  })
}
export function onState(listener){listeners.add(listener);return()=>listeners.delete(listener)}
export async function api(path,method='GET',body){return native('api',{path,method,body})}
