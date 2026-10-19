import { appUrl, isReachable } from './connection.js';

window.location.replace((await isReachable()) ? appUrl : './offline.html');
