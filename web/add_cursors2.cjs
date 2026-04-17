const fs = require('fs');
const path = require('path');

function processFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;

    // 1. Add cursor-pointer to any onClick tag with className={` ... `}
    content = content.replace(/(<(?:button|div|span|a|li|td|tr|p|svg|path)[^\>]+?\bonClick=\{[^\}]+\}[^\>]*?className=\{\`)(.*?)(`\})/g, (match, p1, p2, p3) => {
        if (!p2.includes('cursor-pointer')) {
            return `${p1}${p2} cursor-pointer${p3}`;
        }
        return match;
    });

    // 2. Add cursor-pointer to any <button> that has className={` ... `}
    content = content.replace(/(<button[^\>]*?className=\{\`)(.*?)(`\})/g, (match, p1, p2, p3) => {
        if (!p2.includes('cursor-pointer')) {
            return `${p1}${p2} cursor-pointer${p3}`;
        }
        return match;
    });

    if (content !== original) {
        fs.writeFileSync(filePath, content, 'utf8');
        console.log('Fixed pointers (template literals) in:', filePath);
    }
}

function walkDir(dir) {
    let files = fs.readdirSync(dir);
    for (let f of files) {
        let fullPath = path.join(dir, f);
        if (fs.statSync(fullPath).isDirectory()) {
            walkDir(fullPath);
        } else if (fullPath.endsWith('.tsx')) {
            processFile(fullPath);
        }
    }
}

walkDir(path.join(__dirname, 'src'));