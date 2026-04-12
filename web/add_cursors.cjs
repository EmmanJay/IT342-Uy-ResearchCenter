const fs = require('fs');
const path = require('path');

function processFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;

    // 1. Add cursor-pointer to any tag with onClick that has className="... "
    content = content.replace(/(<(?:button|div|span|a|li|td|tr|p|svg|path)[^\>]+?\bonClick=\{[^\}]+\}[^\>]*?className=)(['"])(.*?)\2/g, (match, p1, p2, p3) => {
        if (!p3.includes('cursor-pointer')) {
            return `${p1}${p2}${p3} cursor-pointer${p2}`;
        }
        return match;
    });

    // 2. Add cursor-pointer to any <button> that has className="... " (even if no onClick)
    content = content.replace(/(<button[^\>]*?className=)(['"])(.*?)\2/g, (match, p1, p2, p3) => {
        if (!p3.includes('cursor-pointer')) {
            return `${p1}${p2}${p3} cursor-pointer${p2}`;
        }
        return match;
    });

    // 3. Add className="cursor-pointer" to <button> or onClick tags that don't have className=""
    // Regex for: tag has onClick=... but DOES NOT contain className=
    // Regex for: tag is <button> but DOES NOT contain className=
    // This is hard with pure regex without lookbehind, so we parse tags.
    
    let parts = content.split('>');
    for (let i = 0; i < parts.length; i++) {
        let p = parts[i];
        if (p.startsWith('<')) {
            let spaceIdx = p.indexOf(' ');
            if (spaceIdx > 0) {
                let tagName = p.substring(1, spaceIdx).trim();
                let attrs = p.substring(spaceIdx);
                
                let isActionable = tagName === 'button' || tagName === 'Link' || attrs.includes('onClick={');
                
                if (isActionable && !attrs.includes('className=') && !attrs.includes('disabled={')) {
                    parts[i] = `<${tagName} className="cursor-pointer"${attrs}`;
                }
            } else if (p === '<button' || p === '<Link') {
                parts[i] = `${p} className="cursor-pointer"`;
            }
        }
    }
    content = parts.join('>');

    if (content !== original) {
        fs.writeFileSync(filePath, content, 'utf8');
        console.log('Fixed pointers in:', filePath);
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
