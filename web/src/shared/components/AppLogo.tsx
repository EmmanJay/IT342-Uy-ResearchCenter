
type AppLogoProps = {
  className?: string;
  markClassName?: string;
  textClassName?: string;
  imageClassName?: string;
  showText?: boolean;
  variant?: 'compact' | 'full';
};

export default function AppLogo({
  className = '',
  markClassName = '',
  textClassName = '',
  showText = true,
  variant = 'compact',
}: AppLogoProps) {
  if (variant === 'full') {
    return (
      <div className={`flex flex-row items-center justify-center ${className}`}>
        <img src="/logo.png" alt="ResearchCenter Logo" className="w-16 h-16 object-contain" />
        <img src="/text.svg" alt="ResearchCenter" className="h-10 mt-2 object-contain" />
      </div>
    );
  }

  return (
    <div className={`inline-flex items-center gap-3 ${className}`}>
      <img src="/logo.png" alt="ResearchCenter Logo" className={`w-10 h-10 object-contain ${markClassName}`} />
      {showText && (
        <img src="/text.svg" alt="ResearchCenter" className={`h-6 object-contain  mt-1.5 ${textClassName}`} />
      )}
    </div>
  );
}
