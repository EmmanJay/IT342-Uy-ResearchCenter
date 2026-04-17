const fs = require('fs');
const path = require('path');

function processFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;

    // Look for tags containing both onClick={...} and className={`...`}
    // Instead of regex order enforcement, find opening tags up to > 
    let changed = false;
    
    let parts = content.split('>');
    for (let i = 0; i < parts.length; i++) {
        let p = parts[i];
        if (p.startsWith('<')) {
            let spaceIdx = p.indexOf(' ');
            if (spaceIdx > 0) {
                let tagName = p.substring(1, spaceIdx).trim();
                let attrs = p.substring(spaceIdx);
                
                let isActionable = tagName === 'button' || tagName === 'Link' || attrs.includes('onClick={');
                
                if (isActionable) {
                    if (attrs.includes('className={`') && !attrs.includes('cursor-pointer')) {
                        // Inject cursor-pointer right before the closing backtick of className={``}
                        parts[i] = p.replace(/(className=\{\`.*?)(\`\})/, '$1 cursor-pointer$2');
                        changed = true;
                    } else if (attrs.includes('className="') && !attrs.includes('cursor-pointer')) {
                        // In case we missed order constraints here too!
                        parts[i] = p.replace(/(className=".*?)(")/, '$1 cursor-pointer$2');
                        changed = true;
                    } else if (attrs.includes("className='") && !attrs.includes("cursor-pointer")) {
                        parts[i] = p.replace(/(className='.*?)(')/, "$1 cursor-pointer$2");
                        changed = true;
                    }
                }
            }
        }
    }
    
    let newContent = parts.join('>');
    if (newContent !== original) {
        fs.writeFileSync(filePath, newContent, 'utf8');
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
