import { useState, FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import Navbar from '../components/Navbar';
import api from '../services/api';

const RegisterPage = () => {
  const navigate = useNavigate();
  const [firstname, setFirstname] = useState('');
  const [lastname, setLastname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (!firstname.trim()) errors.firstname = 'First name is required';
    if (!lastname.trim()) errors.lastname = 'Last name is required';
    if (!email.trim()) errors.email = 'Email is required';
    if (!password) errors.password = 'Password is required';
    else if (password.length < 8) errors.password = 'Password must be at least 8 characters';
    if (password !== confirmPassword) errors.confirmPassword = 'Passwords do not match';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!validate()) return;
    setIsLoading(true);

    try {
      const res = await api.post('/auth/register', { email, password, firstname, lastname });
      localStorage.setItem('accessToken', res.data.data.accessToken);
      localStorage.setItem('refreshToken', res.data.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(res.data.data));
      navigate('/dashboard');
    } catch (err: unknown) {
      const error = err as { response?: { data?: { error?: { code?: string; message?: string } } } };
      const code = error.response?.data?.error?.code;
      if (code === 'AUTH-002') {
        setFieldErrors({ email: 'An account with this email already exists.' });
      } else if (code === 'VALID-001') {
        setError('Please fill in all fields correctly.');
      } else {
        setError('Something went wrong. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F5F5]">
      <Navbar />

      <main className="flex items-center justify-center px-4 py-12">
        <div className="bg-white rounded-xl shadow-sm border border-[#E0E0E0] p-8 w-full max-w-lg">
          <h1 className="text-2xl font-bold text-[#212121]">Create Account</h1>
          <p className="text-sm text-[#757575] mt-1">
            Join ResearchCenter to collaborate on research
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            {/* First + Last Name row */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="firstname" className="block text-sm font-medium text-[#212121] mb-1">
                  First Name
                </label>
                <input
                  id="firstname"
                  type="text"
                  value={firstname}
                  onChange={(e) => setFirstname(e.target.value)}
                  placeholder="John"
                  className="w-full px-3 py-2.5 rounded-md border border-[#E0E0E0] text-sm placeholder-[#9E9E9E] focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:border-transparent bg-white"
                />
                {fieldErrors.firstname && (
                  <p className="text-xs text-[#D32F2F] mt-1">{fieldErrors.firstname}</p>
                )}
              </div>
              <div>
                <label htmlFor="lastname" className="block text-sm font-medium text-[#212121] mb-1">
                  Last Name
                </label>
                <input
                  id="lastname"
                  type="text"
                  value={lastname}
                  onChange={(e) => setLastname(e.target.value)}
                  placeholder="Doe"
                  className="w-full px-3 py-2.5 rounded-md border border-[#E0E0E0] text-sm placeholder-[#9E9E9E] focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:border-transparent bg-white"
                />
                {fieldErrors.lastname && (
                  <p className="text-xs text-[#D32F2F] mt-1">{fieldErrors.lastname}</p>
                )}
              </div>
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-[#212121] mb-1">
                Email Address
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="w-full px-3 py-2.5 rounded-md border border-[#E0E0E0] text-sm placeholder-[#9E9E9E] focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:border-transparent bg-white"
              />
              {fieldErrors.email && (
                <p className="text-xs text-[#D32F2F] mt-1">{fieldErrors.email}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-[#212121] mb-1">
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-3 py-2.5 pr-10 rounded-md border border-[#E0E0E0] text-sm placeholder-[#9E9E9E] focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:border-transparent bg-white"
                />
                <button
                  type="button"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#757575] hover:text-[#212121]"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              <p className="text-xs text-[#757575] mt-1">At least 8 characters</p>
              {fieldErrors.password && (
                <p className="text-xs text-[#D32F2F] mt-0.5">{fieldErrors.password}</p>
              )}
            </div>

            {/* Confirm Password */}
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-[#212121] mb-1">
                Confirm Password
              </label>
              <div className="relative">
                <input
                  id="confirmPassword"
                  type={showConfirm ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-3 py-2.5 pr-10 rounded-md border border-[#E0E0E0] text-sm placeholder-[#9E9E9E] focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:border-transparent bg-white"
                />
                <button
                  type="button"
                  aria-label={showConfirm ? 'Hide password' : 'Show password'}
                  onClick={() => setShowConfirm(!showConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#757575] hover:text-[#212121]"
                >
                  {showConfirm ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {fieldErrors.confirmPassword && (
                <p className="text-xs text-[#D32F2F] mt-1">{fieldErrors.confirmPassword}</p>
              )}
            </div>

            {/* Error */}
            {error && (
              <p className="text-sm text-[#D32F2F]" role="alert">{error}</p>
            )}

            {/* Submit */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-[#2E7D32] hover:bg-[#1B5E20] text-white py-2.5 px-4 rounded-md text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:ring-offset-2 disabled:opacity-60 transition-colors duration-150 min-h-[44px]"
            >
              {isLoading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>

          {/* Divider */}
          <div className="relative my-4">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-[#E0E0E0]" />
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white text-[#757575]">or</span>
            </div>
          </div>

          {/* Google */}
          <button className="w-full bg-white hover:bg-gray-50 text-[#212121] border border-[#E0E0E0] py-2.5 px-4 rounded-md text-sm font-medium transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:ring-offset-2 min-h-[44px]">
            Sign up with Google
          </button>

          <p className="text-center text-sm text-[#757575] mt-4">
            Already have an account?{' '}
            <Link to="/login" className="text-[#2E7D32] font-medium hover:underline">
              Sign In
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
};

export default RegisterPage;
