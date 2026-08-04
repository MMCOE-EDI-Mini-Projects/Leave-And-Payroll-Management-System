import './login.css'
import React, {useState, useEffect} from 'react'
import {Link} from 'react-router-dom'
//import axios from 'axios'
//import { IoMailOpenOutline } from "react-icons/io5";

const Login = ()=>{
    return <>
    <h1 className='hero-text'>MMCOE LEAVE, PAYROLL MANAGEMENT SYSTEM</h1><br></br><br></br>
    <div className="login">
        <form className='userlogin'>
            <h1 className='login-note'>Login</h1><br></br><br></br>
            <label>Username/Email</label><br></br>
            <input type="text" name="username" className='username'></input><br></br><br></br>
            
            <label>Password</label><br></br>
            <input type="password" name="password" className='password'></input><br></br><br></br>

            <button type="submit" className='btn btn-outline-success'>Login</button>

            <br></br><br></br><p>Other options</p>
            <button className='btn btn-outline-dark'><img src='C://codes/cli/dnyatri-react/src/components/login/google-logo-png-hd-11659866438lpwuqaonqq.png' ></img>Login with google</button><br></br><br></br>

            <Link to='/newaccount' className='btn btn-outline-primary'>Create new account</Link>
        </form>
    </div>
    </>
}

export default Login