import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
//import heroImg from './assets/hero.png'
import './App.css'
import Login from './login'
import Signup from './signup'
import {Routes, Route} from 'react-router-dom'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
    {/* <h1>MMCOE</h1> */}
    <Routes>
      <Route path='/' element={<Login></Login>}></Route>
      <Route path='/newaccount' element={<Signup></Signup>}></Route>
    </Routes>
    
    </>
  )
}

export default App
