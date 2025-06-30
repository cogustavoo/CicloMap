CicloMap - Mapeamento Colaborativo de Rotas para Ciclistas
O CicloMap é um projeto de aplicação Android open-source, desenvolvido em Kotlin e Jetpack Compose, com o objetivo de criar uma plataforma de mapeamento colaborativo para ciclistas e utilizadores de transportes alternativos, como patinetes.

O foco inicial do projeto são as cidades de Camboriú e Balneário Camboriú, em Santa Catarina, Brasil.

🎯 O Problema
Muitos ciclistas, especialmente os que usam a bicicleta como principal meio de transporte, enfrentam desafios diários como buracos na via, falta de ciclovias, trechos perigosos ou mal iluminados. Encontrar uma rota segura e eficiente pode ser uma tarefa difícil e arriscada.

💡 A Solução
O CicloMap propõe uma solução colaborativa: uma aplicação onde os próprios utilizadores podem mapear e partilhar informações cruciais sobre as suas rotas. Através de um mapa interativo, um utilizador pode adicionar pontos de alerta, ajudando toda a comunidade a pedalar com mais segurança.

✨ Funcionalidades (Versão Atual - MVP)
Mapa Interativo: Visualização de todos os pontos adicionados pela comunidade em tempo real, utilizando a API do Google Maps.

Adição de Pontos: Um fluxo simples e intuitivo para adicionar novos pontos de alerta:

O utilizador escolhe o tipo de ponto (buraco, perigo, etc.).

Ajusta a localização exata do ponto arrastando um marcador no mapa.

Adiciona observações adicionais.

Ícones Personalizados: Cada tipo de ponto possui um ícone visualmente distinto para fácil identificação no mapa.

Backend com Firebase: Todos os dados são guardados e sincronizados em tempo real com o Firebase Firestore.

🛠️ Tecnologias Utilizadas
Linguagem: Kotlin

Interface de Utilizador: Jetpack Compose

Base de Dados: Firebase Firestore

Autenticação: Firebase Authentication (Anónima)

Mapas: Google Maps SDK para Android

Navegação: Navigation Compose

🚀 Próximos Passos e Funcionalidades Futuras
[ ] Filtros no Mapa: Permitir que os utilizadores filtrem os pontos visíveis por tipo.

[ ] Sistema de Login Completo: Substituir a autenticação anónima por métodos de login reais (Google, Email).

[ ] Ecrã de Perfil Avançado: Mostrar estatísticas e um histórico detalhado das contribuições do utilizador.

[ ] Sistema de Avaliação de Pontos: Permitir que outros utilizadores confirmem ou contestem a validade de um ponto (por exemplo, "este buraco já foi tapado").

Link do APK [Clique aqui](https://drive.google.com/file/d/1YMzHWXtYmzMiiYgvsplIRUxLwS01bbXo/view?usp=drive_link).
