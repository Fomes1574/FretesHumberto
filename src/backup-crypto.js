import {gcm} from '@noble/ciphers/aes.js';
import {pbkdf2Async} from '@noble/hashes/pbkdf2.js';
import {sha256} from '@noble/hashes/sha2.js';
const enc=new TextEncoder();
async function derive(password,salt){return pbkdf2Async(sha256,enc.encode(password),salt,{c:210000,dkLen:32,asyncTick:10})}
export async function encrypt(password,salt,iv,data){const key=await derive(password,salt);try{return gcm(key,iv,enc.encode('FretesHumberto:1')).encrypt(data)}finally{key.fill(0)}}
export async function decrypt(password,salt,iv,data){const key=await derive(password,salt);try{return gcm(key,iv,enc.encode('FretesHumberto:1')).decrypt(data)}finally{key.fill(0)}}
