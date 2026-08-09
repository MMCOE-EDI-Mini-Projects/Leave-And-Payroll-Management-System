import './login.css'
import { Link } from 'react-router-dom'

const Login = () => {
    return (
        <div className='auth-page'>
            <div className='login-card'>
                <div className='form-header'>
                    <h2>Welcome back</h2>
                    <p>Sign in to access the leave and payroll system.</p>
                </div>

                <form className='userlogin'>
                    <label htmlFor='username'>Username / Email</label>
                    <input id='username' type='text' name='username' className='username' placeholder='Enter your username or email' />

                    <label htmlFor='password'>Password</label>
                    <input id='password' type='password' name='password' className='password' placeholder='Enter your password' />

                    <button type='submit' className='btn-submit'>Sign in</button>
                    <Link to='/newaccount' className='form-link'>Create new account</Link>
                </form>
            </div>
        </div>
    )
}

export default Login