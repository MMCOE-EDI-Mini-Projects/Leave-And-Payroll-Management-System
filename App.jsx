import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
//import heroImg from './assets/hero.png'
import './App.css'
import Login from './login'
//import Actual from './components/actual'
//import Signup from './components/login/newaccount'
import {Routes, Route} from 'react-router-dom'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
    {/* <h1>MMCOE</h1> */}
    <Routes>
      <Route path='/' element={<Login></Login>}></Route> 
    </Routes>
    
    </>
  )
}

export default App
