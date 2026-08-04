import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

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
      console.error(error);

      if (error.response) {
        setError(
          error.response.data?.message ||
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

        <h1>ELMS</h1>

        <h2>Employee Leave Management System</h2>

        <p>Login to your account</p>

        <form onSubmit={handleLogin}>

          <div className="form-group">
            <label>Email</label>

            <input
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>Password</label>

            <input
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
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
            {loading ? "Logging in..." : "Login"}
          </button>

        </form>

      </div>
    </div>
  );
}

export default Login;
