import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();

    setError("");
    setLoading(true);

    try {
      const response = await api.post("/auth/login", {
        email: email,
        password: password,
      });

      const data = response.data;

      // Save JWT
      localStorage.setItem("token", data.token);

      // Save user information
      localStorage.setItem("userId", data.userId);
      localStorage.setItem("employeeId", data.employeeId ?? "");
      localStorage.setItem("email", data.email);
      localStorage.setItem("role", data.role);

      // Redirect based on role
      if (data.role === "EMPLOYEE") {
        navigate("/employee");
      } else if (data.role === "MANAGER") {
        navigate("/manager");
      } else if (data.role === "HR") {
        navigate("/hr");
      } else {
        navigate("/");
      }
    } catch (error) {
      if (error.response) {
        setError(
          (typeof error.response.data === "string" ? error.response.data : error.response.data?.message) ||
            "Invalid email or password"
        );
      } else {
        setError(
          "Unable to connect to the server. Make sure the backend is running."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-brand"><span className="brand-mark">E</span><span>ELMS</span></div>
        <p className="eyebrow">Employee Leave Management System</p>
        <h1>Welcome back</h1>
        <p className="login-copy">Sign in to manage leave requests and team availability.</p>

        <form onSubmit={handleLogin}>

          <div className="form-group">
            <label htmlFor="email">Work email</label>

            <input
              type="email"
              id="email"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div className="password-field"><input
              type={showPassword ? "text" : "password"}
              id="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            /><button type="button" className="password-toggle" onClick={() => setShowPassword(!showPassword)}>{showPassword ? "Hide" : "Show"}</button></div>
          </div>

          {error && (
            <p className="error-message">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
          >
            {loading ? "Signing in…" : "Sign in"}
          </button>

        </form>

      </div>
    </div>
  );
}

export default Login;
