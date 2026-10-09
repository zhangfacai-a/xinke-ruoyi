(() => {
  const source='lc-live-customer-v2';
  const emit=(type,payload)=>window.postMessage({source,type,payload,at:Date.now()},location.origin);
  // Only inspect the small set of anchor business endpoints needed by the collector.
  // This avoids parsing unrelated page requests and reduces extension overhead.
  const relevant=(value)=>{try{const path=new URL(String(value),location.href).pathname.toLowerCase();return /\/webcast\/anchor_platform\/api\/v1\/following\/get_online_audiences\/?$/.test(path)}catch(e){return false}};
  const originalFetch=window.fetch;
  window.fetch=async function(...args){const response=await originalFetch.apply(this,args);try{const url=String(args[0]?.url||args[0]||'');if(relevant(url)){const clone=response.clone();const text=await clone.text();if(text.length<1000000)emit('NETWORK_DATA',{url,body:text})}}catch(e){}return response};
  const open=XMLHttpRequest.prototype.open, send=XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open=function(method,url,...rest){this.__lcUrl=String(url);return open.call(this,method,url,...rest)};
  XMLHttpRequest.prototype.send=function(body){this.addEventListener('load',()=>{try{if(relevant(this.__lcUrl||'')&&typeof this.responseText==='string')emit('NETWORK_DATA',{url:this.__lcUrl,body:this.responseText})}catch(e){}});return send.call(this,body)};
})();
