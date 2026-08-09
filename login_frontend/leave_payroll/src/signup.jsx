import './login.css'
import { Link } from 'react-router-dom'

const Signup = () => {
    return (
        <div className='auth-page'>
            <div className='login-card'>
                <div className='form-header'>
                    <h2>Create account</h2>
                    <p>Set up your account to access the leave and payroll system.</p>
                </div>

                <form className='userlogin'>
                    <label htmlFor='fullname'>Full Name</label>
                    <input id='fullname' type='text' name='fullname' className='username' placeholder='Enter your full name' />

                    <label htmlFor='email'>Email Address</label>
                    <input id='email' type='email' name='email' className='username' placeholder='Enter your email address' />

                    <label htmlFor='signup-username'>Username</label>
                    <input id='signup-username' type='text' name='signup-username' className='username' placeholder='Choose a username' />

                    <label htmlFor='signup-password'>Password</label>
                    <input id='signup-password' type='password' name='signup-password' className='password' placeholder='Create a password' />

                    <label htmlFor='confirm-password'>Confirm Password</label>
                    <input id='confirm-password' type='password' name='confirm-password' className='password' placeholder='Confirm your password' />

                    <button type='submit' className='btn-submit'>Create account</button>
                    <Link to='/' className='form-link'>Already have an account? Sign in</Link>
                </form>
            </div>
        </div>
    )
}

export default Signup
