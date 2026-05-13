const http = require('http');
http.get('http://localhost:5173/assets/index-Bxh9weF-.js', res => {
  let d = '';
  res.on('data', chunk => d += chunk);
  res.on('end', () => {
    console.log('FOUND /admin/users:', d.includes('/admin/users'));
    console.log('FOUND create-name:', d.includes('create-name'));
    console.log('FOUND bulk-text:', d.includes('bulk-text'));
    console.log('FOUND AdminUsersPage:', d.includes('AdminUsersPage'));
  });
}).on('error', e => console.error('ERR', e.message));
