import React from 'react';
import { useNavigate } from 'react-router-dom';

interface BreadcrumbItem {
  label: string;
  path?: string;
  state?: any;
}

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
}

const Breadcrumbs: React.FC<BreadcrumbsProps> = ({ items }) => {
  const navigate = useNavigate();

  return (
    <div className="flex items-center text-sm mb-6 overflow-hidden">
      <span 
        onClick={() => navigate('/')} 
        className="text-gray-500 cursor-pointer hover:underline font-medium hover:text-gray-900 flex-shrink-0"
      >
        Home
      </span>
      
      {items.map((item, index) => {
        const isLast = index === items.length - 1;
        return (
          <React.Fragment key={index}>
            <span className="text-gray-400 mx-2 flex-shrink-0">{'>'}</span>
            {isLast || !item.path ? (
              <span className="text-gray-900 font-semibold truncate">
                {item.label}
              </span>
            ) : (
              <span 
                onClick={() => navigate(item.path!, item.state ? { state: item.state } : {})} 
                className="text-gray-500 font-medium hover:underline hover:text-gray-900 cursor-pointer truncate"
                title={item.label}
              >
                {item.label}
              </span>
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};

export default Breadcrumbs;