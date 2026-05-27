import { useCallback, useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import Navbar from '../../shared/components/Navbar';
import { axiosClient } from '../../shared/api/axiosClient';
import { SessionManager } from '../../shared/auth/sessionManager';

type InvitePreview = {
  repositoryId: string | number;
  repositoryName: string;
  status: string;
  email: string;
};

const getErrorResponse = (error: unknown) => {
  return error as {
    response?: {
      status?: number;
      data?: { message?: string; error?: { message?: string } };
    };
  };
};

const AcceptInvitePage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState<'checking' | 'ready' | 'accepting' | 'success' | 'error'>('checking');
  const [message, setMessage] = useState('');
  const [invite, setInvite] = useState<InvitePreview | null>(null);
  const navigate = useNavigate();

  const returnToLogin = useCallback(() => {
    if (!token) return;
    SessionManager.clear();
    navigate('/login', {
      replace: true,
      state: { returnTo: `/invite/accept?token=${encodeURIComponent(token)}` },
    });
  }, [navigate, token]);

  useEffect(() => {
    const checkInvite = async () => {
      if (!token) {
        setStatus('error');
        setMessage('Missing invite token');
        return;
      }
      if (!SessionManager.getToken()) {
        returnToLogin();
        return;
      }

      try {
        setStatus('checking');
        const resp = await axiosClient.get(`/invitations/${token}`);
        setInvite(resp.data?.data || null);
        setStatus('ready');
      } catch (error: unknown) {
        const errorResponse = getErrorResponse(error);
        const statusCode = errorResponse.response?.status;
        if (statusCode === 401 || statusCode === 403) {
          returnToLogin();
          return;
        }
        setStatus('error');
        setMessage(errorResponse.response?.data?.message || errorResponse.response?.data?.error?.message || 'Invitation not found');
      }
    };

    checkInvite();
  }, [returnToLogin, token]);

  const acceptInvite = async () => {
    if (!token) return;
    try {
      setStatus('accepting');
      const resp = await axiosClient.post(`/invitations/${token}/accept`);
      const data = resp.data?.data || {};
      setStatus('success');
      setMessage(data.message || 'Invitation accepted');
      setTimeout(() => navigate(data.repositoryId ? `/repositories/${data.repositoryId}` : '/dashboard'), 700);
    } catch (error: unknown) {
      const errorResponse = getErrorResponse(error);
      const statusCode = errorResponse.response?.status;
      if (statusCode === 401 || statusCode === 403) {
        returnToLogin();
        return;
      }
      setStatus('error');
      setMessage(errorResponse.response?.data?.message || errorResponse.response?.data?.error?.message || 'Failed to accept invitation');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm bg-white/30 px-4">
        <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 text-center border border-gray-100">
          <h2 className="text-2xl font-bold text-gray-900 mb-3">Repository Invitation</h2>

          {status === 'checking' && <p className="text-gray-600">Checking invitation...</p>}

          {status === 'ready' && (
            <>
              <p className="text-sm text-gray-600 mb-6">
                Accept the invitation to join <span className="font-semibold text-gray-900">{invite?.repositoryName || 'this repository'}</span>
                {invite?.email ? <> as <span className="font-medium">{invite.email}</span></> : null}.
              </p>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => navigate('/dashboard')}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={acceptInvite}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 cursor-pointer"
                >
                  Accept Invite
                </button>
              </div>
            </>
          )}

          {status === 'accepting' && <p className="text-gray-600">Accepting invitation...</p>}
          {status === 'success' && <p className="text-green-600 font-semibold">{message}</p>}
          {status === 'error' && <p className="text-red-600">{message}</p>}
        </div>
      </div>
    </div>
  );
};

export default AcceptInvitePage;
