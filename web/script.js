const fs = require('fs');
const path = require('path');

function processFile(file) {
    if (!file.endsWith('.tsx') && !file.endsWith('.ts')) return;
    let content = fs.readFileSync(file, 'utf8');

    // Regex to match buttons, a tags, or any tag with onClick
    // It's tricky to write perfect regex for jsx/tsx, but we can do a simplified regex
    // We look for className="..." in <button ...>, <a ...>, or <... onClick=...>
    
    // Instead of parsing perfectly, let's just use some naive regex:
    // Match any <button ... className="..."> 
    // And any element with onClick={...} and className="..."
    // Wait, a better way: Find all elements that look like button or a or have onClick
    // Since this is JSX, let's just find className="([^"]*)" and see if the tag needs cursor-pointer.
    // Let's iterate over every tag manually using a simple tokenizer or regex.
    
    let regex = /<([A-Za-z0-9_]+)(\s[^>]*)>/g;
    
    let newContent = content.replace(regex, (match, tag, attrs) => {
        if (tag === 'button' || tag === 'a' || tag === 'Link' || tag === 'NavLink' || attrs.includes('onClick=')) {
            if (!attrs.includes('className=')) {
                // If no className, it might be tricky to inject. But usually we have className in tailwind code
                return < className="cursor-pointer">;
            } else {
                // It has className
                return match.replace(/className="([^"]*)"/, (clsMatch, clsVal) => {
                    if (clsVal.includes('cursor-pointer') || clsVal.includes('cursor-not-allowed')) {
                        return clsMatch;
                    }
                    return className="\ cursor-pointer";
                });
            }
        }
        return match;
    });

    if (newContent !== content) {
        fs.writeFileSync(file, newContent, 'utf8');
        console.log('Updated', file);
    }
}

function traverse(dir) {
    fs.readdirSync(dir).forEach(file => {
        let full = path.join(dir, file);
        if (fs.statSync(full).isDirectory()) {
            traverse(full);
        } else {
            processFile(full);
        }
    });
}

traverse(path.join(__dirname, 'src'));
