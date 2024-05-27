def project = "yoda"
switch(project) {
    case "yoda":
        println (["cruzverde", "maicao", "yza", "fybeca", "okidoki", "sanasana"])
        break
    case "chewbacca":
        return ["farmashop", "oxxo-chile", "oxxo-colombia", "oxxo-peru", "farmacenter", "gama"]
        break
   case "kenobi":
        return ["chilexpress"]
        break
   
    default:
        return []
}