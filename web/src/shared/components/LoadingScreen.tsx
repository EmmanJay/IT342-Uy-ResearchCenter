import AppLogo from './AppLogo';

type LoadingScreenProps = {
  label?: string;
  fullScreen?: boolean;
  className?: string;
};

export default function LoadingScreen({ label = 'Loading workspace', fullScreen = true, className = '' }: LoadingScreenProps) {
  return (
    <div className={`${fullScreen ? 'min-h-screen' : 'min-h-[220px]'} ${className} flex items-center justify-center px-6`}>
      <div className="flex flex-col items-center text-center">
        <div className="relative mb-5 flex h-16 w-16 items-center justify-center">
          <div className="rc-loader-ring absolute inset-0 rounded-full border-4 border-dashed" />
        </div>
        <p className="rc-loader-text text-sm font-medium text-center" style={{ color: '#212121' }}>
          {label}
        </p>
      </div>
    </div>
  );
}
