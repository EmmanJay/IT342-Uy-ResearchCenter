import React from 'react';

interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  confirmColor?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  confirmColor = 'bg-red-600 hover:bg-red-700',
  onConfirm,
  onCancel
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/40 backdrop-blur-sm">
      <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4 shadow-2xl border border-gray-100">
        <h3 className="text-xl font-bold text-gray-900 mb-2">{title}</h3>
        <p className="text-gray-600 mb-6">{message}</p>
        <div className="flex justify-end gap-3">
          <button 
            onClick={onCancel} 
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
          >
            {cancelText}
          </button>
          <button 
            onClick={onConfirm} 
            className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors ${confirmColor}`}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;