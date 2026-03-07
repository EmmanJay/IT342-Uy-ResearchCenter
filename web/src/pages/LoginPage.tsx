import { useState, FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import Navbar from '../components/Navbar';
import api from '../services/api';

const LoginPage = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const res = await api.post('/auth/login', { email, password });
      localStorage.setItem('accessToken', res.data.data.accessToken);
      localStorage.setItem('refreshToken', res.data.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(res.data.data));
      navigate('/dashboard');
    } catch (err: unknown) {
      const error = err as { response?: { data?: { error?: { code?: string } } } };
      const code = error.response?.data?.error?.code;
      if (code === 'AUTH-001') {
        setError('Invalid email or password. Please try again.');
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

      <main className="flex items-center justify-center px-4 py-16">
        <div className="bg-white rounded-xl shadow-sm border border-[#E0E0E0] p-8 w-full max-w-md">
          <h1 className="text-2xl font-bold text-[#212121]">Sign In</h1>
          <p className="text-sm text-[#757575] mt-1">
            Enter your credentials to access your account
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
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
                required
                className="w-full px-3 py-2.5 rounded-md border border-[#E0E0E0] text-sm placeholder-[#9E9E9E] focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:border-transparent bg-white"
              />
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
                  required
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
              <div className="flex justify-end mt-1">
                <a href="#" className="text-sm text-[#2E7D32] hover:underline font-medium">
                  Forgot Password?
                </a>
              </div>
            </div>

            {/* Error message */}
            {error && (
              <p className="text-sm text-[#D32F2F]" role="alert">{error}</p>
            )}

            {/* Submit */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-[#2E7D32] hover:bg-[#1B5E20] text-white py-2.5 px-4 rounded-md text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:ring-offset-2 disabled:opacity-60 transition-colors duration-150 min-h-[44px]"
            >
              {isLoading ? 'Signing in...' : 'Sign In'}
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
            Sign in with Google
          </button>

          <p className="text-center text-sm text-[#757575] mt-4">
            Don't have an account?{' '}
            <Link to="/register" className="text-[#2E7D32] font-medium hover:underline">
              Sign Up
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
};

export default LoginPage;
