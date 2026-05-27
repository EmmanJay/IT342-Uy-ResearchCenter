import React from 'react';

const ResearchLoader = ({ label = 'Loading...' }: { label?: string }) => {
  return (
    <div className="flex flex-col items-center justify-center p-8 bg-transparent">
      <div className="relative flex items-center justify-center w-24 h-24">
        <div className="absolute inset-0 rounded-full border-4 border-dashed border-green-500 animate-[spin_3s_linear_infinite]" />
        <div className="w-12 h-12 rounded-xl flex items-center justify-center shadow-md bg-green-700 animate-pulse">
          <div className="flex flex-col gap-1 w-5">
            <div className="h-1 bg-white rounded w-full" />
            <div className="h-1 bg-white rounded w-3/4" />
            <div className="h-1 bg-white rounded w-1/2" />
          </div>
        </div>
      </div>
      <p className="mt-6 text-sm font-medium tracking-wide text-center text-gray-700 animate-pulse">
        {label}
      </p>
    </div>
  );
};

export default ResearchLoader;